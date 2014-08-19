#!/usr/bin/python
#
# Copyright (c) 2013 Big Switch Networks, Inc.
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

# Simple command description builder.
#

import os
import copy
import fileinput
import traceback
import argparse

def parse(*line):
    if args.p:
        print line

def debug(*line):
    if args.d:
        print line

def verbose(*line):
    if args.v:
        print line

def error(*line):
    print line


add_token = ('o', '+')
front_token = ('o', '^')
end_token = ('o', '$')

def tokens(line):
    """
    Returns the tokenzied line, with a eol token at the end
    The start-token must be prefixed before parsing
    """
    tokens = []
    i = 0
    end = len(line)
    while i < end:
        # break
        while line[i] in ' \t\r\n':
            i += 1
            if i == end:
                break
        if i == end:
            break
        if line[i] in '[]{}<>()|':
            tokens.append(('o', line[i])) # 'o' <= char, op
            i += 1
            continue
        # span.
        token = ''
        run_i = i 
        while not line[run_i] in ' \t[]{}<>|\n':
            run_i += 1
            if run_i == end:
                break
        tokens.append(('t', line[i:run_i])) # 't' <= token
        i = run_i
    #
    tokens.append(end_token) # 't' <= '$' for eol

    return tokens

priority_dict = {
           '^'  : 0,       # hat, front of line
           '$'  : 1,       # newline, end of line
           ')'  : 2,
           '('  : 3,
           '}'  : 4,
           '{'  : 5,
           ']'  : 6,
           '['  : 7,
           '>'  : 8,
           '<'  : 9,
           '|'  : 10,
           '+'  : 11,
           }

def priority(tok):
    if not is_op(tok):
        return -1 # lower than any with a valid priority, enables push
    if not tok[1] in priority_dict:
        return 100 # higher than any with a valid priority, enables push
    return priority_dict[tok[1]]

def is_op(tok):
    return tok[0] == 'o'

def is_token(tok):
    return tok[0] == 't'

def is_field(tok):
    return tok[0] == 'field'

def is_tree(tok):
    return tok[0] == 'tree'

def is_list(tok):
    return type(tok) == list

def op_of(tok):
    return tok[1]

def token_of(tok):
    return tok[1]

def field_list_of(tok):
    return tok[1]

balance = {
        ']'  : '[',
        '}'  : '{',
        '>'  : '<',
        ')'  : '(',
        '$'  : '^',
}

def partner(tok):
    if is_op(tok):
        if token_of(tok) in balance:
            return balance[token_of(tok)]
    return None


stack = []

def print_stack():
    for s in stack:
        print s

def push(item):
    parse("PUSH ", stack, "ADD ", item)
    stack.append(item)

def pop():
    parse( "POP", stack )
    p = stack[-1]
    del stack[-1]
    return p

def reset_stack():
    global stack
    stack = []

def peek(i = 1):
    top = len(stack)
    if top < i:
        return ('o','^') # <= front of line
    return stack[-i]

def is_in_order(collect):
    if is_op(collect[1]) and collect[1][1] == '+':
        return True
    return False

def is_either(collect):
    if is_op(collect[1]) and collect[1][1] == '|':
        return True
    return False

def gather_field_list(collect, field_list):
    if type(collect) == list:
        for f in collect:
            gather_field_list(f, field_list)
    elif is_token(collect):
        field_list.append(collect)
    elif is_tree(collect):
        gather_field_list(token_of(token_of(collect)), field_list)
    else:
        field_list.append(field_list)

def gather_field(collect):
    parse( "GATHER FIELD ", collect)
    field_list = []
    if is_token(collect):
        field_list.append(collect)
    elif is_tree(collect):
        gather_field_list(token_of(token_of(collect)), field_list)
    elif type(collect) == list:
        gather_field_list(collect, field_list)
    else:
        field_list.append(collect)
    return ('field', field_list)

def tree_builder(collect, tok):
    result = None
    op = op_of(tok)
    parse( "WHAT ", collect, tok)
    if op == '}': # { stuff } ... select one from the args
        # XXX early return
        if len(collect) == 1:
            result = ('CHOICE ALONE', collect)
        elif is_either(collect):
            result = ("CHOICE OF", [c for c in collect if not is_op(c)])
        elif is_in_order(collect):
            result = ("CHOICE ORDER", [c for c in collect if not is_op(c)])
        elif is_tree(collect):
            result = ('CHOICE TREE', collect)
        elif is_field(collect):
            return gather_field(collect)
        elif is_token(collect):
            return collect
        elif is_token(collect):
            return collect
        else:
            result = ("CHOICE TROUBLE", op, collect)
    elif op == ']': # [ stuff ] ... stuff which is optional
        if len(collect) == 1:
            result = ('OPTION ALONE', collect)
        elif is_either(collect):
            result = ("OPTION OF", [c for c in collect if not is_op(c)])
        elif is_in_order(collect):
            result = ("OPTION ORDER", [c for c in collect if not is_op(c)])
        elif is_tree(collect):
            result = ('OPTION TREE', collect)
        elif is_field(collect):
            return ('tree', ('OPTION FIELD', collect))
        elif is_token(collect):
            result = ('OPTION TOKEN', collect)
        else:
            result = ("OPTION TROUBLE", op, collect)
    elif op == ')': # ( stuff ) ... no semantic meaning,
        # XXX early return
        return collect
    elif op == '>': # description of a field
        gather = gather_field(collect)
        parse("GATHERED: ",gather)
        return gather
    elif op == '|': # either of
        result = ('EITHER', [c for c in collect if not is_op(c)])
    elif op == '+': # sum of
        result = ('ORDER', [c for c in collect if not is_op(c)])
    elif op == '$': # eol, collect up any tree's left
        # XXX syntax error?
        print "STACK "
        print_stack()
        print "TOK ", tok
        print "COLLECT", collect
        exit()
    else:
        parse('return collect, later tok', tok)
        return collect
    parse( "BUILD ", op, type(result))
    return ('tree', result)


def single_token(tok):
    (which, t) = tok
    parse( "NEXT", which, t, peek())

    if is_token(tok) and is_token(peek()):
        # two tokens in a row, pretend the op is '+'
        push(add_token)

    # is this a <tree><tree> ?
    if is_tree(peek()) and (is_tree(peek(2)) or is_field(peek(2))):
        # collect together as many as possible
        collect = [pop()]
        while is_tree(peek()) or is_field(peek()):
            collect.insert(0, pop())
        push(tree_builder(collect, add_token))
    # is this a <tree><tree> ?
    elif (not is_op(peek())) and (not is_op(peek(2))):
        # collect together as many as possible
        collect = [pop()]
        while not is_op(peek()):
            collect.insert(0, pop())
        push(tree_builder(collect, add_token))

    if is_op(tok):
        if not is_op(peek(1)): # item or token or field
            parse( 'PRIO ', tok, priority(tok), peek(2), priority(peek(2)))
            while priority(tok) < priority(peek(2)):
                # collect as many from the same priority
                last = pop()
                parse( "-->", stack, tok, last)
                # uniary op?
                if is_op(peek()) and partner(tok) == op_of(peek()):  
                    parse( "UNIARY ")
                    pop() # <= pop matching op
                    push(tree_builder(last, tok)) # <= token popped off
                    parse( "LEAVE", stack, tok)
                    return # don't push the uniary right op

                collect = last
                op = tok
                if is_op(peek()):
                    op = peek()
                    parse( "BINARY ", op)
                    collect = [last]
                    while is_op(peek()) and \
                      not (partner(tok) == op_of(peek())) and \
                      priority(op) == priority(peek()):
                        parse( "WHY ", op_of(op), priority(op), op_of(peek()), priority(peek()))
                        collect.insert(0, pop()) # <= op
                        collect.insert(0, pop()) # <= token
                    if len(collect) == 1:
                        print "NOT BINARY", tok, op, peek
                        exit()
                parse( "==> ", collect, tok)
                parse( "__  ", stack)
                push(tree_builder(collect, op))
                parse( "SO FAR ", stack)
                parse( "OP FAR ", tok)
    push(tok)
    parse( "LAST", stack, tok)
 

#
def single_line(tokens):
    reset_stack()
    for (which, t) in tokens:
        single_token((which, t))

def all_tokens(token_list):
    for token in token_list:
        parse( "ALL TOKEN? ", token)
        if not is_token(token):
            return False
    return True


class printer:
    def __init__(self, form, is_optional, indent = 2):
        self.form = form
        self.is_optional = is_optional
        self.need_optional = False
        self.indent = indent
    
    def __str__(self):
        return "form %s need %s optional %s indent %s" % (
            self.form, self.need_optional, self.is_optional, self.indent)
    
    def indents(self, extra = 0):
        return "    " * (self.indent + extra)
    
    def to_dict(self):
        self.form = 'dict'

    def is_dict(self):
        if self.form == 'dict':
            return True
        return False

    def to_tuple(self):
        self.form = 'tuple'

    def is_tuple(self):
        if self.form == 'tuple':
            return True
        return False

    def more_indent(self, incr = 1):
        self.indent += incr
    
    def less_indent(self, decr = 1):
        self.indent -= decr
    
    def nest(self, incr = 1, form = None, is_optional = None):
        new = copy.deepcopy(self)
        new.indent += 1
        if form:
            new.form = form
        if is_optional:
            new.is_optional = is_optional
        return new

class description:
    def __init__(self):
        self.desc = []
        
    def raw_out(self, line):
        self.desc.append(line)

    def out(self, line, printer):
        self.desc.append(printer.indents() + line)

    def out_with_indent(self, line, printer):
        self.desc.append(printer.indents(1) + line)

    def result(self):
        return '\n'.join(self.desc)

desc = description()

def single_recurse(tree):
    """
    look for nested trees whose leaf only has a single element
    """
    return False

def maker_in_order(in_order, printer):
    debug( "IN_ORDER ", is_tree(in_order), in_order )
    if is_list(in_order):
        was_dict = False
        desc.out('# %d items in order' % len(in_order) , printer)
        desc.out('# %s ' % printer , printer)
        if printer.is_dict():
            printer.to_tuple()
            desc.out("(", printer)
            printer.more_indent()
            was_dict = True
        do_optional = False
        if printer.is_optional:
            if not was_dict:
                desc.out("(", printer)
                printer.more_indent()
            do_optional = True
            save_need_optional = printer.need_optional
            printer.need_optional = True
            printer.is_optional = False
                
        for (n, tree) in enumerate(in_order, 1):
            debug( "IN_ORDER ITEM ", n, is_token(tree), tree)
            desc.out('# item %d %s' % (n, printer) , printer)
            maker_items(tree, printer)

        if was_dict or do_optional:
            printer.less_indent()
            desc.out("),", printer)
            if was_dict:
                printer.to_dict()
            if do_optional:
                printer.is_optional = True
                printer.need_optional = save_need_optional

    elif is_tree(in_order):
        was_dict = False
        if printer.is_dict():
            desc.out("(", printer) # )(
            desc.out("#in_order2", printer)
            traceback.print_stack()
            printer.to_tuple()
            printer.more_indent()
            was_dict = True
        debug( "IN_ORDER TREE ", token_of(in_order) )
        maker_do_op(token_of(in_order), printer)
        if was_dict:
            printer.less_indent()
            desc.out("),", printer)
            printer.to_dict()
    elif is_token(in_order):
        maker_items(in_order, printer.nest(incr = 1))
    elif is_field(in_order):
        maker_field(field_list_of(in_order), printer.nest(incr = 1))
    else:
        error( "IN_ORDER STUCK" )
    
def maker_field(field_list, printer):
    was_tuple = False
    if printer.is_tuple:
        was_tuple = True
        desc.out("{", printer)
        printer.more_indent()
        printer.to_tuple()

    if printer.need_optional:
        desc.out("'optional' : %s," % printer.is_optional, printer)

    for field in field_list:
        # Add more items here to provide more field decoration
        printer.more_indent
        value = token_of(field)
        if value.find('=') == -1:
            desc.out("'field' : '%s'," % value, printer)
        else:
            desc.out("'%s' : '%s'," % tuple(value.split('=')), printer)
        printer.less_indent
        
    if was_tuple:
        printer.less_indent()
        desc.out( "},", printer )
        printer.to_dict()

def maker_choice(tree_tuple, printer):
    debug( 'MAKER_CHOICE', tree_tuple, printer.indent )

    if is_tree(tree_tuple):
        # XXX some tree's can be squashed.
        debug( "MAKER_CHOICE ITEM ", tree_tuple )
        maker_do_op(token_of(tree_tuple), printer)
        return

    # choice needs to print a dictionary.
    was_tuple = False
    if printer.is_tuple():
        printer.to_dict()
        was_tuple = True

    desc.out('{', printer)
    printer.more_indent()

    if printer.is_optional:
        desc.out("'optional': %s," % printer.is_optional,  printer)
    desc.out("'choices' : (", printer)
    desc.out("    # maker_choice", printer)

    if is_list(tree_tuple):
        debug( "CHOICE LIST", len(tree_tuple), tree_tuple )
        printer.more_indent()
        for (n, item) in enumerate(tree_tuple, 1):
            debug( "CHOICE LIst #%d" % n )
            debug( "       ITEM ", item )
            maker_items(item, printer)
        printer.less_indent()
    elif is_tree(tree_tuple):
        debug( "CHOICE TREE" )
        (tree_which, tree) = tree_tuple[1]
        # tree_which == 'tree'
        maker_do_op(token_of(tree_tuple), printer.nest(form = 'tuple', incr = 1))
    elif is_field(tree_tuple):
        debug( "CHOICE FIELD", tree_tuple[1] )
        printer.more_indent()
        maker_field(field_list_of(tree_tuple), printer)
        printer.less_indent()
    else:
        error( 'MAKER_CHOICE CONFUSED' )

    desc.out(")", printer)
    printer.less_indent()
    desc.out('},', printer)

    if was_tuple:
        printer.to_tuple()


def maker_do_op(op_tuple, printer):
    debug( 'OP=> ', op_tuple )
    (op, operands) = op_tuple
    debug( 'OP ', op_tuple, op, operands )
    if op == 'ORDER':
        debug( "OP IN_ORDER ", operands )
        maker_in_order(operands, printer)
    elif op == 'EITHER':
        # XXX wrong
        maker_choice(operands, printer)
    elif op.startswith('CHOICE'):
        maker_choice(operands, printer)
    elif op.startswith('OPTION'):
        was_optional = printer.is_optional
        printer.is_optional = True
        debug( 'OP OPTIONAL', operands )
        maker_items(operands, printer)
        printer.is_optional = was_optional


def maker_trees(trees, printer):
    (tree_which, op) = trees
    maker_do_op(op, printer)


def maker_items(items, printer):
    debug( "ITEMS-> ", type(items), items )
    if type(items) == list:
        for item in items:
            if is_tree(item):
                debug( "TREE ", item )
                maker_do_op(item, printer)
            elif is_field(item):
                desc.out("{", printer)
                maker_field(field_list_of(item), printer)
                if printer.need_optional:
                    desc.out("'optional' : %s," % printer.is_optional, printer)
                desc.out( "},", printer)
            elif is_token(item):
                desc.out("{", printer)
                printer.more_indent()
                desc.out("'token' : '%s'," % token_of(item), printer)
                if printer.need_optional:
                    desc.out("'optional' : %s," % printer.is_optional, printer)
                printer.less_indent()
                desc.out("},", printer)
    elif is_tree(items):
        maker_do_op(token_of(items), printer)
    elif is_field(items):
        maker_field(field_list_of(items), printer)
    elif is_token(items):
        debug( 'ITEMS TOKEN', items )
        desc.out( "{", printer)
        printer.more_indent()
        desc.out( "'token' : '%s'," % token_of(items), printer)
        if printer.need_optional:
            desc.out( "'optional' : %s," % printer.is_optional, printer)
        printer.less_indent()
        desc.out( "},", printer)
    else:
        error( "ITEMS> STUCK", items )
    

#
def maker_args(args, printer):
    debug( "MAKER_ARGS ", args )
    (pick, item) = args
    if pick == 'EITHER':
        debug( "MAKER_ARGS EITHER ", item )
        if len(item) >= 1:
            desc.raw_out( '    args : {')
            if all_tokens(item):
                for choice in item:
                    desc.raw_out("            '%s'," % token_of(choice, 3))
            else:
                maker_trees(item, printer)
            desc.raw_out( '    },' )
        else:  # exactly one choice
            desc.raw_out( '    args : { ' )
            if all_token(item):
                desc.raw_out("            '%s'," % token_of(item[0]))
            else:
                print maker_trees(item, printer)
            desc.raw_out( '    },' )
    elif pick.startswith('CHOICE'):
        debug( "CHOICE", len(item) )
        if len(item) == 1:
            maker_args(item)
        elif is_tree(item) and token_of(item)[0] == 'EITHER':
            maker_choice(token_of(item), printer)
        elif is_field(item) or is_token(item):
            maker_choice(item, printer)
        else:
            error( "CHOICE HELP ", item )
    elif pick.startswith('ORDER'):
        # ought to choose the form of the printer based on item
        desc.out("'args' : {", printer)
        printer.less_indent()
        maker_in_order(item, printer)
        printer.less_indent()
        desc.out('}', printer)
    elif pick.startswith('OPTION'):
        printer.is_optional = True
        desc.raw_out( '     args : {')
        maker_trees(item, printer)
        desc.raw_out( '    },')
    else:
        error( "MAKER_PICKER HELP ", pick )

saved_input = []

#
def maker(name_tuple, no_supported, result, printer):

    name = token_of(name_tuple)
    verbose( 'Name: %s no %s Result %s' % (name, no_supported, result) )

    type = 'add-command-type'
    mode = 'login'
    new_mode = 'config-CHANGE'
    obj_type = None
    #
    # command-name@command-type@command-mode@obj-type@new_mode
    #
    if name.find('@') >= 0:
        name_parts = name.split('@')
        name = name_parts[0]
        type = name_parts[1]
        if len(name_parts) > 2:
            mode = name_parts[2]
        if len(name_parts) > 3:
            obj_type = name_parts[3]
        if len(name_parts) > 4:
            new_mode = name_parts[4]
        # name-value pairs?

    debug( "NAME ", (name) )
    debug( 'command name ',  name )
    desc.raw_out( '%s_%s_COMMAND_DESCRIPTION = {' % 
                  (args.n.upper(), name.replace("-","_").upper()))
    desc.raw_out( "    'name'          : '%s'," % name)
    desc.raw_out( "    'mode'          : '%s'," % mode)
    if no_supported == False:
        desc.raw_out( "    'no-supported'  : False,")
    desc.raw_out( "    'command-type'  : '%s'," % type)
    if obj_type:
        desc.raw_out( "    'obj-type'      : '%s'," % obj_type)
    if type == 'config-submode':
        desc.raw_out( "    'submode-name'  : '%s'," % new_mode)
        desc.raw_out( "    'parent-id'     : None,")
    desc.raw_out( "    'short-help'    : 'add-short-command-help',")
    if args.f:
        desc.raw_out( "    'feature'       : '%s'," % args.f)
    # if the remaining length is two, ORDER should be popped.
    desc.raw_out( "    'args' : ( ")
    maker_items(result, printer)
    desc.raw_out( "    ),")
    desc.raw_out( "}")
    return

    if len(order_list) == 2 and len(order) == 1:
        build_choice = None
        if is_tree(order_list[1]):
            debug( "MAKER TREE.", token_of(order_list[1]) )
            if token_of(order_list[1])[0].startswith('CHOICE'):
                choice = token_of(order_list[1])
                build_choice = order_list[1]
        if build_choice:
            desc.out("'args' : {", printer)
            printer.more_indent()
            maker_choice(build_choice, printer)
            printer.less_indent()
            desc.out( '},', printer)
        else:
            print "XXX", order_list
            print "XXX", order_list[1]
            desc.out("'args' : (", printer)
            printer.more_indent()
            printer.to_tuple()
            maker_in_order(order_list[1], printer)
            printer.less_indent()
            desc.out( '),', printer)
    elif len(order) > 1:
        maker_args((order[0], order[1:]), printer)
    else:
        desc.raw_out('}')


parser = argparse.ArgumentParser(prog='desc_maker')
parser.add_argument('file')
parser.add_argument('-p')
parser.add_argument('-d')
parser.add_argument('-v')
parser.add_argument('-n', default = "NEW")
parser.add_argument('-f')
args = parser.parse_args()

for line in fileinput.input(args.file):

    line_tokens = tokens(line)
    if len(line_tokens) < 2: # why 2? even blank lines get eol
        saved_input.append( "# @ %s" %  line)
        continue
    #
    # remove '[' 'no' ']' if its there, don't forget the leading '^'
    no_supported = False

    if len(line_tokens) > 1:
        if token_of(line_tokens[0]) == '#':
            saved_input.append( "# @ %s" %  line)
            continue
        elif token_of(line_tokens[0]) == '[':
            if len(line_tokens) > 2:
                if token_of(line_tokens[1]) == 'no':
                    if len(line_tokens) > 3:
                        if token_of(line_tokens[2]) == ']':
                            if len(line_tokens) > 4:
                                no_supported = True
                                name = line_tokens[3]
                                parse_tokens = line_tokens[4:]
                        else:
                            print 'Warning: name required after \[ no \']'
                            continue
                    else:
                        print 'Warning: only \'\[ no \]\' allowed as prefix'
                        continue
                else:
                    print 'Warning: only \'\[ no \]\' allowed as prefix'
                    continue
            else:
                print 'Warning: only single \[ in line'
                continue
        else:
            name = line_tokens[0]
            parse_tokens = line_tokens[1:]

    saved_input.append( "# @ %s" %  line)
    single_line([front_token] + parse_tokens)

    # should look like ^ tree $
    if len(stack) == 3 and not is_op(stack[1]):
        debug( "OK------------------  -> ", token_of(stack[1]) )
        a_printer = printer('tuple', is_optional = False)
        desc.out('\n#\n# %s#\n' % line, a_printer)
        maker(name, no_supported, stack[1], a_printer)
    else:
        #
        # Could peek at the stack to get an idea of the nature of the syntax error
        print "SYNTAX ERROR", name, stack

#
#

print "#"
print "# Command used as input for this run are listed below,"
print "# Fish the command out by egreping '^# @' "
print "#"

print ''.join(saved_input)
print desc.result()
