#
# Copyright (c) 2010,2011,2012,2013 Big Switch Networks, Inc.
#
# Licensed under the Eclipse Public License, Version 1.0 (the
# "License"); you may not use this file except in compliance with the
# License. You may obtain a copy of the License at
#
#      http://www.eclipse.org/legal/epl-v10.html
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
# implied. See the License for the specific language governing
# permissions and limitations under the License.
#

#
# PRETTYPRINT
#
# This module contains classes that help formatting text for the CLI,
# including tables, individual records etc.
# 
# Formatting information is stored in table_info, a dict of dicts.

import model_info_list
import os
import array
import datetime
import utif
import re
import fmtcnv

class PrettyPrinter():
    table_info = None   # Annotated table format. This is a dict of dicts. Set by cli.

    def __init__(self, bs):
        self.sdnsh = bs
        self.table_info = {}
        self.format_version = {}
        self.format_added_modules = {}

        self.add_format('MODEL',
                        'model_info',
                        model_info_list.model_info_dict)


    def add_format(self, name, origin, format_dict):
        """
        Add a format to the saved formats, from the dictionary
        with 'name'.   The 'name' can help identify the source
        for this format when errors arise
        """

        # very interesting items in a format are the field_orderings,
        # and the fields description.   It could make sense to only
        # allow specific items from the fields, to prevent field-output
        # formatting from using uninnteded database fields.
        for (format_name, format) in format_dict.items():
            if not format_name in self.table_info:
                self.table_info[format_name] = { }
            # combine details: top level items
            for (item_name, item_value) in format.items():
                if item_name == 'fields':
                    if not item_name in self.table_info[format_name]:
                        self.table_info[format_name][item_name] = {}
                    for (term_name, term_value) in item_value.items():
                        if not term_name in self.table_info[format_name][item_name]:
                            self.table_info[format_name][item_name][term_name] = {}
                        self.table_info[format_name][item_name][term_name].update(term_value)
                if item_name == 'field-orderings':
                    if not item_name in self.table_info[format_name]:
                        self.table_info[format_name][item_name] = {}
                    self.table_info[format_name][item_name].update(item_value)
            #
            # save the name of the source for this format
            if not 'self' in self.table_info[format_name]:
                self.table_info[format_name]['self'] = name
            if not 'origin' in self.table_info[format_name]:
                self.table_info[format_name]['origin'] = origin
            #
            # Add the 'Idx' field if it's not there.
            if format_name in self.table_info:
                if not 'fields' in self.table_info[format_name]:
                    self.table_info[format_name]['fields'] = {}
                if not 'Idx' in self.table_info[format_name]['fields']:
                    self.table_info[format_name]['fields']['Idx'] = {
                            'verbose-name' : '#',
                            'type' : 'CharField'
                        }


    def add_module_name(self, version, module):
        """
        Save some state about the version/module
        """
        if version not in self.format_version:
            self.format_version[version] = [module]
        else:
            self.format_version[version].append(module)


    def add_formats_from_module(self, version, module):
        self.add_module_name(version, module)
        for name in dir(module):
            if re.match(r'.*_FORMAT$', name):
                if module.__name__ not in self.format_added_modules:
                    self.format_added_modules[module.__name__] = [name]
                if name not in self.format_added_modules[module.__name__]:
                    self.format_added_modules[module.__name__].append(name)
                self.add_format(name, module.__name__, getattr(module, name))


    # Utility Functions
    def get_field_info(self, obj_type_info, field_name):
        return obj_type_info['fields'].get(field_name, None)


    def format_to_alias_update(self, display_format, update):
        """
        Given a format, find all the formatters, and for each
        formatter, determine which aliases need to be updated.
        """
        if display_format not in self.table_info:
            # this will fail soon enough when final output is attempted
            return
        format_dict = self.table_info[display_format]
        if not 'fields' in format_dict:
            return

        for (field_name, field_dict) in format_dict['fields'].items():
            if 'formatter' in field_dict:
                fmtcnv.formatter_to_alias_update(field_dict['formatter'], update)
            elif 'entry_formatter' in field_dict:
                fmtcnv.formatter_to_alias_update(field_dict['entry-formatter'], update)
        return update


    def formats(self):
        return self.table_info.keys()

    def format_details(self):
        """
        Return a table of formats suitable as a format_table parameter
        """
        return [ { 'format'      : x,
                   'format dict' : self.table_info[x]['self'],
                   'origin'      : self.table_info[x]['origin'] }

                  for x in self.formats()]


    def format_as_header(self, field):
        # LOOK! could prob do something fancy to handle camelCapStrings or underscore_strings
        return field[0].capitalize() + field[1:]


    def get_header_for_field(self, obj_type_info, field_name):
        field_info = self.get_field_info(obj_type_info, field_name)
        if not field_info:
            return self.format_as_header(field_name)
        default_header = self.format_as_header(field_name)
        return field_info.get('verbose-name', default_header)


    def format_table(self, data_list, display_format = None, field_ordering="default"):
        """
        Takes in list of dicts and generates nice table, e.g.

        id slice_id MAC Address       ip    Switch ID    
        ------------------------------------------------
        1  1        00:00:00:00:01:03 10013 150861407404
        2  2        00:00:00:00:01:01 10011 150861407404
        3  1        00:00:00:00:02:03 10023 150866955514

        @param data_list - a list of dicts
        @param format describes the format of the output table to display
        @param field_ordering is the field_ordering identifier in the model,
          there can be multiple ("default", "brief", "detailed")
       
        """

        #
        # first, determine a list of fields to be printed, then using that list,
        # determine the fields width of each field, including calling any formatting
        # function, then format the result
        #
        # during the format computation, replace the value with the 'formatted value'
        # to prevent multiple calls to the same formatting funcition
        #
        if not data_list or not type(data_list) == list:
            if type(data_list) == dict and "error_type" in data_list:
                return data_list.get("description", "Internal error")
            return "None."

        format_info = self.table_info.get(display_format, None)

        if self.sdnsh.description:
            if format_info == None:
                print 'format_table: missing format %s' % display_format
            else:
                format_from = format_info.get('self', '')
                print 'format_table: %s %s %d entries' % (
                        display_format, format_from, len(data_list))

        field_widths = {}
        field_headers = {}
        fields_to_print = []

        #
        # do the 'figur'n for which fields will get printed.
        #
        # to determine the length, call the formatting function, and replace the
        # value for that field with the updated value; then 'cypher the length.
        #
        # note that field_widths.keys() are all the possible fields
        # check if the headers makes any field wider and set fields_to_print
        #
        if format_info:
            if 'field-orderings' in format_info:
                fields_to_print = format_info['field-orderings'].get(field_ordering, [])
            if len(fields_to_print) == 0: # either no field_orderings or couldn't find specific
                fields_to_print = format_info['fields'].keys()
            for f in fields_to_print:
                header = self.get_header_for_field(format_info, f)
                field_headers[f] = header
                field_widths[f] = max(len(header), field_widths.get(f, 0))
            # LOOK! not done now... add in extra fields discovered in data_list if desired
            # right now, fields_to_print is a projection on the data
        else:
            # get fields_to_print from the field names in data_list,
            # which is (intended to be) a list of dictionaries
            all_fields = utif.unique_list_from_list(sum([x.keys()
                                                        for x in data_list], []))
            fields_to_print = sorted(all_fields)

        if self.sdnsh.description:
            print 'format_table: field order "%s" fields %s' % \
                   (field_ordering, fields_to_print)

        #
        # generate a fields_to_print ordered list with field_widths for each
        # by going through all data and then using field_ordering if avail.
        #
        row_index = 0
        for row in data_list:
            row_index += 1
            if not 'Idx' in row:
                row['Idx'] = row_index
            for key in fields_to_print:
            #for (k,v) in row.items():
                if format_info:
                    # don't worry about header here - do that soon below
                    info = self.get_field_info(format_info, key)
                    if info and info.get('formatter'):
                        row[key] = str(info['formatter'](row.get(key, ''), row))
                        w = len(row[key])
                    else:
                        w = len(str(row.get(key, '')))
                else:
                    field_headers[key] = self.format_as_header(key)
                    w = max(len(str(row[key])), len(field_headers[key]))
                field_widths[key] = max(w, field_widths.get(key, 0))

        #
        # generate the format_str and header lines based on fields_to_print
        #
        format_str_per_field = []
        for f in fields_to_print:
            format_str_per_field.append("%%(%s)-%ds" % (f, field_widths[f]))

        row_format_str = " ".join(format_str_per_field) + "\n"

        #
        # finally print! only caveat is to handle sparse data with a blank_dict
        # let result be a list, and append new strings to generate the final result,
        # (for better python performance)
        #
        result= []
        result.append(" ".join(format_str_per_field) % field_headers + "\n")
        result.append("|".join(["-"*field_widths[f] for f in fields_to_print]) + "\n") # I <3 python too

        blank_dict = dict([(f,"") for f in fields_to_print])
        for row in data_list:
            result.append(row_format_str % dict(blank_dict, **row))
        
        return ''.join(result)


    def format_entry(self, data, display_format=None, field_ordering="default", debug=False):
        """
        Takes in parsed JSON object, generates nice single entry printout,
        intended for 'details' display

        @param data list of dictionaries, values for output
        @param format name of format description to use for printing
        @param field_ordering list of field to print
        @param debug print values of compound keys
        """
        if not data:
            return "None."
        elif type(data) == dict and "error_type" in data:
            return data.get("description", "Internal error")
    
        format_info = self.table_info.get(display_format, None)

        # Print. Pretty please.
        if format_info:
            fields = format_info['fields']
        else:
            fields = dict([[x, {}] for x in data.keys()])
            format_info = { 'fields' : fields }
            if self.sdnsh.description:
                print "format_entry: Missing format ", display_format, fields

        # Find widest data field name
        label_w = len( max(data, key=lambda x:len(x)) )
        if format_info:
            verbose_len = max([len(self.get_header_for_field(format_info, x)) for x in fields.keys()])
            # This isn't exactly right, the verbose names for the fields ought to be replaced first
            label_w = max(verbose_len, label_w)
        label_str = "%%-%ds :" % label_w

        # Use format_info for this table to order fields if possible
        fields_to_print = None
        if format_info:
            if 'field-orderings' in format_info:
                fields_to_print = format_info['field-orderings'].get(field_ordering, [])
            else:
                if self.sdnsh.description:
                    print 'Error: internal: %s field ordering %s not present for %s' % \
                          (display_format, field_ordering, format_info)
            if fields_to_print == None or len(fields_to_print) == 0:
                # either no field_orderings or couldn't find specific
                fields_to_print = format_info['fields'].keys()
        else:
            fields_to_print = sorted(fields.keys())

        result = ""
        tmp_merged_dict = dict([(f,"") for f in fields_to_print], **data)
        blank_dict = dict([(f,"") for f in fields_to_print])

        # first print the requested fields
        all_fields_in_correct_order = list(fields_to_print)
        # then the remaining fields
        all_fields_in_correct_order.extend([x for x in fields.keys()
                                            if x not in fields_to_print])
        # give all the formatter's a shot, save the updates
        updated = {}
        for e in all_fields_in_correct_order:
            if format_info:
                info = self.get_field_info(format_info, e)
                if not info:
                    continue
                if 'entry-formatter' in info:
                    updated[e] = info['entry-formatter'](
                                    tmp_merged_dict.get(e, ''), tmp_merged_dict)
                elif 'formatter' in info:
                    updated[e] = info['formatter'](
                                    tmp_merged_dict.get(e, ''), tmp_merged_dict)

        tmp_merged_dict.update(updated)

        data.update(updated)
        all_fields_in_correct_order = filter(lambda x: x in data,
                                             all_fields_in_correct_order)
        for e in all_fields_in_correct_order:
            if format_info:
                info = self.get_field_info(format_info, e)
                if not debug and info and 'help_text' in info and info['help_text'][0] == '#':
                    # sdnsh._is_compound_key(), please skip display of compound key
                    continue
            result += (label_str % self.get_header_for_field(format_info, e)+" "+
                       str(tmp_merged_dict[e]) + "\n");
        
        return result[:-1]

    def get_terminal_size(self):
        def ioctl_GWINSZ(fd):
            try:
                import fcntl, termios, struct, os
                cr = struct.unpack('hh', fcntl.ioctl(fd, termios.TIOCGWINSZ,
                                                     '1234'))
            except:
                return None
            return cr
        cr = ioctl_GWINSZ(0) or ioctl_GWINSZ(1) or ioctl_GWINSZ(2)
        if not cr:
            try:
                fd = os.open(os.ctermid(), os.O_RDONLY)
                cr = ioctl_GWINSZ(fd)
                os.close(fd)
            except:
                pass
        if not cr:
            try:
                cr = (os.environ['LINES'], os.environ['COLUMNS'])
            except:
                cr = (24, 80)

        if (cr[1] == 0 or cr[0] == 0):
            return (80, 24)

        return int(cr[1]), int(cr[0])

    def format_time_series_graph(self, data, obj_type=None, field_ordering="default"):
        if not data:
            return "None."
        elif type(data) == dict and "error_type" in data:
            return data.get("description", "Internal error")

        obj_type_info = self.table_info.get(obj_type, None)

        if self.sdnsh.description:
            print "format_time_series_graph", obj_type, obj_type_info
        yunits = None
        ylabel = "Value"
        if (obj_type_info):
            ylabel = obj_type_info['fields']['value']['verbose-name']
            if 'units' in obj_type_info['fields']['value']:
                yunits = obj_type_info['fields']['value']['units']


        miny = None
        maxy = None
        minx = None
        maxx = None

        for (x,y) in data:
            if miny == None or y < miny:
                miny = y
            if maxy == None or y > maxy:
                maxy = y
            if minx == None or x < minx:
                minx = x
            if maxx == None or x > maxx:
                maxx = x

        if (yunits == '%'):
            maxy = 100

        if isinstance(maxy, float) and maxy < 10.0:
            axisyw = len('%.5f' % maxy)
        else:
            axisyw = len('%s' % maxy)
        
        (twidth, theight) = self.get_terminal_size()

        width = twidth-axisyw
        height = theight-5

        ybucket = float(maxy)/height;

        xbucket = (maxx-minx)/width;
        if (xbucket == 0):
            minx = maxx - 1800000
            maxx += 1800000
            xbucket = maxx/width;

        graph = array.array('c')
        graph.fromstring(' ' * (width * height))
        for (x,y) in data:
            if (ybucket == 0):
                yc = height
            else:
                yc = int(round((maxy-y)/ybucket))
            if (xbucket == 0):
                xy = width
            else:
                xc = int(round((x-minx)/xbucket))

            if (yc < 0):
                yc = 0
            if (yc >= height):
                yc = height-1
            if (xc < 0):
                xc = 0
            if (xc >= width):
                xc = width-1

            #print (xc,yc, x, y, yc*width + xc)
            for i in range(yc,height):
                graph[i*width + xc] = '#'

        b = '%s\n' % (ylabel)

        if isinstance(maxy, float) and maxy < 10.0:
            form = '%%%d.5f|%%s\n'
        else:
            form = '%%%ds|%%s\n'

        for i in range(0,height-1):
            ylabel = maxy - i*ybucket
            if not isinstance(maxy, float) or maxy >= 10.0:
                ylabel = int(round(ylabel))
            b += (form % axisyw) % \
                (ylabel, 
                 ''.join(graph[i*width:(i+1)*width-1]))
        b += (form % axisyw) % \
            (0, ''.join(graph[(height-1)*width:height*width-1]).replace(' ', '_'))

        b += '%s' % (' ' * axisyw)
        d = ' ' * axisyw

        olddate = None
        interval = (maxx - minx)/(width/7.0)
        for i in range(0, width/7):
            curtimestamp = minx + interval*i
           
            if i == width/7-1:
                df = (' ' * (width % 7)) + " %m/%d^"
                tf = (' ' * (width % 7)) + " %H:%M^"
                curtimestamp = maxx
            else:
                df = "^%m/%d "
                tf = "^%H:%M "

            curtime = datetime.datetime.fromtimestamp(curtimestamp/1000.0)
            date = curtime.strftime(df)
            b += curtime.strftime(tf)
            if (date != olddate):
                olddate = date
                d += date
            else:
                d += ' ' * 7

        b += '\n%s\n' % d
        b += '%s%sTime' % (' ' * axisyw, ' ' * (width/2-2))

        return b
