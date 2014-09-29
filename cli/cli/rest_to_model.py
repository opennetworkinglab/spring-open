#
# Copyright (c) 2012,2013 Big Switch Networks, Inc.
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

import collections
import urllib
import error
import json
import modi
import utif
import url_cache

#
# json based on non model based query
#

def rest_to_model_init(bs, modi):
    global sdnsh, mi

    sdnsh = bs
    mi = modi

#
# --------------------------------------------------------------------------------

def check_rest_result(result, message = None):
    if isinstance(result, collections.Mapping):
        error_type = result.get('error_type')
        if error_type:
            raise error.CommandRestError(result, message)


#
# --------------------------------------------------------------------------------

def get_model_from_url(obj_type, data):
    """
    Intended to be used as a conversion tool, to provide a way
    to move model requests to specific url requests
    """

    onos = 1
    startswith = '__startswith'
    data = dict(data)

    if mi.obj_type_has_model(obj_type):
        print "MODEL_URL: %s not supported" % obj_type
        return []
    #
    # save sort field
    sort = data.get('orderby', None)
    if sort:
        del data['orderby']

    obj_type_url = mi.obj_type_has_url(obj_type)
    url = "http://%s/rest/v1/" % sdnsh.controller + obj_type_url

    # for the devices query, be sensitive to the types of queries
    # since the devices query doesn't understand the relationship
    # between the vlan=='' for address_space == 'default'.
    if obj_type_url == 'device':
        if 'vlan' in data and data['vlan'] == '':
            del data['vlan']

    # data items need to be included into the query.
    if data:
        url += '?'
        url += urllib.urlencode(data)
    if sdnsh.description:   # description debugging
        print "get_model_from_url: request ", obj_type, data, url
    #
    # cache small, short time results
    entries = url_cache.get_cached_url(url)
    if entries != None:
        if sdnsh.description:   # description debugging
            print 'get_model_from_url: using cached result for ', url
    else:
        try:
            result = sdnsh.store.rest_simple_request(url)
            check_rest_result(result)
            entries = json.loads(result)
        except Exception, e:
            if sdnsh.description:   # description debugging
                print "get_model_from_url: failed request %s: '%s'" % (url, e)
            entries = []
        
        url_cache.save_url(url, entries)

    key = mi.pk(obj_type)

    # convert specific types XXX need better generic mechanism
    result = []
    if obj_type == 'host-attachment-point':
        for entry in entries:
            # it is possible for multiple mac's to appear here 
            # as long as a single ip is associated with the device.
            if len(entry['mac']) > 1:
                raise error.CommandInternalError("host (attachment point): >1 mac")
            mac = entry['mac'][0]
            aps = entry['attachmentPoint']
            lastseen = entry['lastSeen']
            for ap in aps:
                status = ap.get('errorStatus', '')
                if status == None:
                    status = ''
                if len(entry['vlan']) == 0:
                    result.append({'id'           : mac + '|' + ap['switchDPID'] +
                                                          '|' + str(ap['port']),
                                   'mac'          : mac,
                                   'switch'       : ap['switchDPID'],
                                   'ingress-port' : ap['port'],
                                   'status'       : status,
                                   'last-seen'    : lastseen,
                                   'address-space': entry['entityClass'],
                                   })
                else:
                    for vlan in entry['vlan']:
                        result.append({'id'           : mac + '|' + ap['switchDPID'] +
                                                              '|' + str(ap['port']),
                                       'mac'          : mac,
                                       'vlan'         : vlan,
                                       'switch'       : ap['switchDPID'],
                                       'ingress-port' : ap['port'],
                                       'status'       : status,
                                       'last-seen'    : lastseen,
                                       'address-space': entry['entityClass'],
                                       })
    elif obj_type == 'host-network-address':

        for entry in entries:
            if len(entry['mac']) > 1:
                raise error.CommandInternalError("host (network-address): >1 mac")
            mac = entry['mac'][0]
            ips = entry['ipv4']
            lastseen = entry['lastSeen']
            for ip in ips:
                if len(entry['vlan']) == 0:
                    result.append({'id'            : mac + '|' + ip,
                                   'mac'           : mac,
                                   'ip-address'    : ip,
                                   'address-space' : entry['entityClass'],
                                   'last-seen'     : lastseen })
                else:
                    for vlan in entry['vlan']:
                        result.append({'id'            : mac + '|' + ip,
                                       'mac'           : mac,
                                       'vlan'          : vlan,
                                       'ip-address'    : ip,
                                       'address-space' : entry['entityClass'],
                                       'last-seen'     : lastseen })
    elif obj_type == 'host':
        # For host queries, the device manager returns 'devices' which
        # match specific criteria, for example, if you say ipv4 == 'xxx',
        # the device which matches that criteris is returned.  However,
        # there may be multiple ip addresses associated.
        #
        # this means that two different groups of values for these
        # entities are returned.  fields like 'ipv4' is the collection
        # of matching values, while 'ips' is the complee list.
        # 'switch', 'port' are the match values, while 'attachment-points'
        # is the complete list.
        #

        ip_match = data.get('ipv4')
        ip_prefix = data.get('ipv4__startswith')

        dpid_match = data.get('dpid')
        dpid_prefix = data.get('dpid__startswith')

        port_match = data.get('port')
        port_prefix = data.get('port__startswith')

        address_space_match = data.get('address-space')
        address_space_prefix = data.get('address-space__startswith')

        for entry in entries:
            if onos==0:
                if len(entry['mac']) > 1:
                    raise error.CommandInternalError("host: >1 mac")
                mac = entry['mac'][0]
                lastseen = entry['lastSeen']
            else:
                mac = entry['mac']
                lastseen = 0

            ips = None
            if not ip_match and not ip_prefix:
                ipv4 = entry['ipv4']
            elif ip_match:
                ipv4 = [x for x in entry['ipv4'] if x == ip_match]
            elif ip_prefix:
                ipv4 = [x for x in entry['ipv4'] if x.startswith(ip_prefix)]

            if len(entry['ipv4']):
                ips = [{'ip-address' : entry['ipv4'], 'last-seen' : lastseen}]
                    	    #for x in entry['ipv4'] ]
            aps = None
            switch = []
            port = []

            #dpid_match = entry.get('dpid')
            if onos == 0:
                attachPoint = 'attachmentPoint'
                attachDpid = 'switchDPID'
                attachPort = 'port'
            else:
                attachPoint = 'attachmentPoints'
                attachDpid = 'dpid'
                attachPort = 'portNumber'

            if len(entry[attachPoint]):
                aps = [{'switch' : x[attachDpid], 'ingress-port' : x[attachPort] }
                       for x in entry[attachPoint]]

                if not dpid_match and not dpid_prefix:
                    switch = [x[attachDpid] for x in entry[attachPoint]]
                elif dpid_match:
                    switch = [x[attachDpid] for x in entry[attachPoint]
                              if x[attachDpid] == dpid_match]
                elif dpid_prefix:
                    switch = [x[attachDpid] for x in entry[attachPoint]
                              if x[attachDpid].startswith(dpid_prefix)]

                if not port_match and not port_prefix:
                    port = [x[attachPort] for x in entry[attachPoint]]
                elif port_match:
                    port = [x[attachPort] for x in entry[attachPoint]
                            if x[attachPort] == port_match]
                elif port_prefix:
                    port = [x[attachPort] for x in entry[attachPoint]
                            if x[attachPort].startswith(port_prefix)]

            if onos == 0:
                address_space = entry['entityClass']
                dhcp_client_name = entry.get('dhcpClientName', '')
                if address_space_match and address_space != address_space_match:
                    continue
                if address_space_prefix and not address_space.startswith(address_space_prefix):
                    continue

            if onos == 1:
                id = '%s' % (mac)
                result.append({'id'                : id,
                           'mac'               : mac,
                           'ips'               : ips,
                           'ipv4'              : ipv4,
                           'attachment-points' : aps,
                           'dpid'              : switch,
                           'port'              : port,
                           'last-seen'         : 0})
            else:
                if len(entry['vlan']) == 0:
                    id = '%s||%s' % (address_space, mac)
                    result.append({'id'                : id,
                               'mac'               : mac,
                               'ips'               : ips,
                               'ipv4'              : ipv4,
                               'attachment-points' : aps,
                               'dpid'              : switch,
                               'port'              : port,
                               'address-space'     : address_space,
                               'dhcp-client-name'  : dhcp_client_name,
                               'last-seen'         : lastseen})
                else:
                    for vlan in entry['vlan']:
                        if address_space != 'default':
                            id = '%s||%s' % (address_space, mac)
                        else:
                            id = '%s|%s|%s' % (address_space, vlan, mac)
                    result.append({'id'                : id,
                                   'mac'               : mac,
                                   'vlan'              : vlan,
                                   'ips'               : ips,
                                   'ipv4'              : ipv4,
                                   'attachment-points' : aps,
                                   'dpid'              : switch,
                                   'port'              : port,
                                   'address-space'     : address_space,
                                   'dhcp-client-name'  : dhcp_client_name,
                                   'last-seen'         : lastseen})

        # Also need to add hostConfig entries.
        if not mi.obj_type_has_model('host-config'):
            raise error.CommandInternalError("host-config: not served via model")
        host_config = sdnsh.rest_query_objects('host-config', data)
        if sdnsh.description:   # description debugging
            print "get_model_from_url: adding host-config ", data, host_config
        known_ids = [x['id'] for x in result]

        for hc in host_config:
            id = hc['id']
            if id not in known_ids:
                # be sensitive to search fields:
                query_match = True
                if len(data):
                    for (d, dv) in data.items():
                        # other ops aside from '='?
                        if d.endswith(startswith):
                            fn = d[:-len(startswith)]
                            if not fn in hc or not hc[fn].startswith(dv):
                                query_match = False
                        elif (not d in hc) or dv != hc[d]:
                            query_match = False
                            break
                            
                if query_match:
                    hc['attachment-points'] = None
                    hc['dpid'] = None
                    hc['ips'] = None
                    hc['last-seen'] = None
                    result.append(hc)

    elif obj_type == 'vns-interface':
        for entry in entries:
            vns = entry['parentVNS']['name']
            rule = entry['parentRule']
            rule_name = 'default'
            if rule and 'name' in rule:
                rule_name = rule['name']

            result.append({ key            : vns + '|' + entry['name'],
                           'vns'           : vns,
                           'address-space' : entry['parentVNS']['addressSpaceName'],
                           'interface'     : entry['name'],
                           'rule'          : rule_name,
                           'last-seen'     : entry['lastSeen'],
                          })

        # also need to add vns-interface-config
        if not mi.obj_type_has_model('vns-interface-config'):
            raise error.CommandInternalError("vns-interface-config: not service via model")
        vns_intf_config = sdnsh.rest_query_objects('vns-interface-config', data)
        known_vns_intfs = [x[key] for x in result]
        for vns_intf in vns_intf_config:
            if not vns_intf[key] in known_vns_intfs:
                vns_intf['rule'] = 'default'
                result.append(vns_intf)

    elif obj_type == 'host-vns-interface':

        # mac matching works for the request

        vns_match = data.get('vns')
        vns_prefix = data.get('vns__startswith')

        for entry in entries:
            device = entry['device']
            if len(device['mac']) > 1:
                raise error.CommandInternalError("host (vns-interface): >1 mac")

            device_last_seen = device['lastSeen']
            address_space = device['entityClass']
            mac = device['mac'][0]          # currently, should only be one

            ips = None
            if len(device['ipv4']):
                ips = [{'ip-address' : x, 'last-seen' : device_last_seen}
                        for x in device['ipv4'] ]

            vlans = device.get('vlan', [])   # currently, should only be one
            if len(vlans) == 0:              # iterate once when list is empty
                vlans = [ '' ]

            aps = None
            if len(device['attachmentPoint']):
                aps = [{'switch' : x['switchDPID'], 'ingress-port' : x['port'] }
                       for x in device['attachmentPoint']]

            for iface in entry.get('iface', []):
                vns = iface['parentVNS']['name']
                last_seen = iface['lastSeen']
                if vns_match and vns_match != vns:
                    continue
                if vns_prefix and not vns.startswith(vns_prefix):
                    continue

                for vlan in vlans: # there's supposed to only be at most one vlan.
                    if address_space != 'default' or type(vlan) != int:
                        host = '%s||%s' % (address_space, mac)
                    else:
                        host = '%s|%s|%s' % (address_space, vlan, mac)

                    result.append({key             : mac + '|' + vns + '|' + iface['name'],
                                   'host'          : host,
                                   'mac'           : mac,
                                   'vlan'          : vlan,
                                   'address-space' : address_space,
                                   'ips'           : ips,
                                   'attachment-points' : aps,
                                   'vns'               : vns,
                                   'interface'         : vns + '|' + iface['name'],
                                   'last-seen'         : device_last_seen})
    elif obj_type == 'switches':
        switch_match = data.get('dpid')
        switch_prefix = data.get('dpid__startswith')

        # this synthetic obj_type's name is 'switches' in an attempt
        # to disabigutate it from 'class Switch'
        #TODO: Need figure out a better way to get url (Through sdncon framework)
        url = "http://%s/rest/v1/mastership" % sdnsh.controller
        try:
            result2 = sdnsh.store.rest_simple_request(url)
            check_rest_result(result2)
            mastership_data = json.loads(result2)
        except Exception, e:
            if sdnsh.description:   # description debugging
                print "get_model_from_url: failed request %s: '%s'" % (url, e)
            entries = []
        for entry in entries:
            dpid = entry.get('dpid')
            if(dpid in mastership_data.keys()):
                #As there is only one master for switch
                controller = mastership_data[dpid][0].get('controllerId')
            else:
                controller = None
            if switch_match and switch_match != entry['dpid']:
                continue
            if switch_prefix and not entry['dpid'].startswith(switch_prefix):
                continue
            if onos == 1:
                result.append({
                   'dpid'                : entry['dpid'],
                   'switch-alias'        : entry['stringAttributes']['name'],
                   'connected-since'     : entry['stringAttributes']['ConnectedSince'],
                   'ip-address'          : entry['stringAttributes']['remoteAddress'],
                   'type'                : entry['stringAttributes']['type'],
                   'controller'          : controller
                })
            else:
                attrs = entry['attributes']
                actions = entry['actions']
                capabilities = entry['capabilities']
                inet_address = entry.get('inetAddress')
                ip_address = ''
                tcp_port = ''
                if inet_address:
                    # Current Java value looks like: /192.168.2.104:38420
                    inet_parts = inet_address.split(':')
                    ip_address = inet_parts[0][1:]
                    tcp_port = inet_parts[1]
    
                result.append({
                   'dpid'                : entry['dpid'],
                   'connected-since'     : entry['connectedSince'],
                   'ip-address'          : ip_address,
                   'tcp-port'            : tcp_port,
                   'actions'             : actions,
                   'capabilities'        : capabilities,
                   'dp-desc'             : attrs.get('DescriptionData', ''),
                   'fast-wildcards'      : attrs.get('FastWildcards', ''),
                   'supports-nx-role'    : attrs.get('supportsNxRole', ''),
                   'supports-ofpp-flood' : attrs.get('supportsOfppFlood', ''),
                   'supports-ofpp-table' : attrs.get('supportsOfppTable', ''),
                   'core-switch'         : False,
                })
        # now add switch-config

        switch_config = sdnsh.rest_query_objects('switch-config', data)
        known_dpids = dict([[x['dpid'], x] for x in result])

        if onos == 0:
            for sw in switch_config:
                dpid = sw['dpid']
                if not dpid in known_dpids:
                    # be sensitive to search fields:
                    query_match = True
                    if len(data):
                        for (d, dv) in data.items():
                            # other ops aside from '='?
                            if d.endswith(startswith):
                                fn = d[:-len(startswith)]
                                if not fn in sw or not sw[fn].startswith(dv):
                                    query_match = False
                            elif (not d in sw) or dv != sw[d]:
                                query_match = False
                                break
    
                    if query_match:
                        sw['ip-address'] = ''
                        sw['tcp-port'] = ''
                        sw['connected-since'] = ''
                        result.append(sw)
                [dpid].update(sw)
    elif obj_type == 'interfaces':

        # These are called interfaces because the primary
        # key is constructed with the interface name, not
        # the port number.

        # the 'switches' query to sdnplatform currently
        # doesn't support searching for the interface
        # names.  its done here instead

        switch_match = data.get('dpid')
        switch_prefix = data.get('dpid__startswith')

        name_match = data.get('portName')
        if name_match:
            name_match = name_match.lower()
        name_prefix = data.get('portName__startswith')
        if name_prefix:
            name_prefix = name_prefix.lower()

        # this synthetic obj_type's name is 'switches' in an attempt
        # to disabigutate it from 'class Switch'
        for entry in entries:
            dpid = entry['dpid']

            if switch_match and switch_match != dpid:
                continue
            if switch_prefix and not dpid.startswith(switch_prefix):
                continue

            for p in entry['ports']:
                portNumber = p['portNumber']
                if onos == 0:
                    name = p['name']
                else:
                    name = p['stringAttributes']['name']

                if name_match and name.lower() != name_match:
                    continue
                if name_prefix and not name.lower().startswith(name_prefix):
                    continue
                if onos == 0:
                    result.append({
                                    'id'                 : '%s|%s' % (dpid,name),
                                    'portNumber'         : portNumber,
                                    'switch'             : dpid,
                                    'portName'           : p['name'],
                                    'config'             : p['config'],
                                    'state'              : p['state'],
                                    'advertisedFeatures' : p['advertisedFeatures'],
                                    'currentFeatures'    : p['currentFeatures'],
                                    'hardwareAddress'    : p['hardwareAddress'],
                                  })
                else:
                    result.append({
                                    'id'                 : '%s|%s' % (dpid,name),
                                    'portNumber'         : portNumber,
                                    'switch'             : dpid,
                                    'portName'           : name,
                                    'config'             : 0,
                                    'state'              : p['state'],
                                    'advertisedFeatures' : 0,
                                    'currentFeatures'    : 0,
                                    'hardwareAddress'    : 0,
                                  })
                

    #
    # order the result
    if sort:
        if sort[0] == '-':
            sort = sort[1:]
            # decreasing
            if sdnsh.description:   # description debugging
                print "get_model_from_url: order decreasing ", sort
            result = sorted(result, key=lambda k:k.get(sort, ''),
                                    cmp=lambda x,y : cmp(y,x))
        else:
            # increasing
            if sdnsh.description:   # description debugging
                print "get_model_from_url: order increasing ", sort
            result = sorted(result, key=lambda k:k.get(sort, ''),
                                    cmp=lambda x,y : cmp(x,y))
    else:
        # use tail-integer on the entries
        if sdnsh.description:   # description debugging
            print "get_model_from_url: pk ordering ", key
        def sort_cmp(x,y):
            for (idx, x_v) in enumerate(x):
                c = cmp(utif.try_int(x_v), utif.try_int(y[idx]))
                if c != 0:
                    return c
            return 0
        result = sorted(result, key=lambda k:k.get(key, '').split('|'),
                                cmp=lambda x,y : cmp(utif.try_int(x),utif.try_int(y)))

    if sdnsh.description:   # description debugging
        print "get_model_from_url: result ", obj_type, url, len(result)

    return result


def validate_switch():
    """
    If /rest/v1/switches is cached, perform some validations on it.

    -- verify that the announced interfaces names are case insensitive
    -- verify the names only appear once
    """

    def duplicate_port(entry, name):
        dpid = entry['dpid']

        print 'Warning: switch %s duplicate interface names: %s' % (dpid, name)
        if sdnsh.debug_backtrace:
            for port in entry['ports']:
                if port['name'] == name:
                    print 'SWTICH %s:%s PORT %s' %  (entry, name, port)

    def not_case_sensitive(entry, name):
        dpid = entry['dpid']

        ports = {}
        for port in entry['ports']:
            if port['name'].lower() == name:
                ports[port['name']] = port
            
        print 'Warning: switch %s case insentive interface names: %s' % \
               (dpid, ' - '.join(ports.keys()))
        if sdnsh.debug_backtrace:
            for port in ports:
                print 'SWTICH %s PORT %s' % (dpid, port)

    url = "http://%s/rest/v1/switches" % sdnsh.controller
    entries = url_cache.get_cached_url(url)
    if entries:
        for entry in entries:
            dpid = entry['dpid']

            # verify that the port names are unique even when case
            # sensitive
            all_names = [p['name'] for p in entry['ports']]
            one_case_names = utif.unique_list_from_list([x.lower() for x in all_names])
            if len(all_names) != len(one_case_names):
                # Something is rotten, find out what.
                for (i, port_name) in enumerate(all_names):
                    # use enumerate to drive upper-triangle comparison
                    for other_name in all_names[i+1:]:
                        if port_name == other_name:
                            duplicate_port(entry, port_name)
                        elif port_name.lower() == other_name.lower():
                            not_case_sensitive(entry, port_name)
                    

