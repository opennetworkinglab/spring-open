#
# Copyright (c) 2011,2012,2013 Big Switch Networks, Inc.
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
# UTIlity Functions
#

import re


#
# --------------------------------------------------------------------------------

def is_power_of_two(n):
    """
    Return true if the integer parameter is a power of two
    """
    if (n & (n - 1)) == 0:
        return True
    return False

#
# --------------------------------------------------------------------------------

def unique_list_from_list(dup_list):
    """
    Return a new list from the old, where the the new has no repeated items.
    (the items are intended to be strings)
    """
    return dict([[x, None] for x in dup_list]).keys()

#
# --------------------------------------------------------------------------------

def full_word_from_choices(word, all_choices):
    """
    given a single word, which could be a prefix of a word in
    a list (which is the second parameter), return a single
    word which matches or None, when non or more than one match.
    """
    choices = [x for x in all_choices if x.startswith(word)]
    if len(choices) == 1:
        return choices[0]
    if word in all_choices:      # also allow an exact match
        return word
    return None


#
# --------------------------------------------------------------------------------

def try_int(string):
    """
    Return an interger if possible, otherwise a string
    """
    try:
        str_int = int(string)
        return str_int
    except:
        pass
    return string

#
# --------------------------------------------------------------------------------

def mask_to_cidr(mask):
    """
    Given a mask used in AC's, for eample 0.0.0.255, return the cidr /<n>
    value associated with that mask.  The mask must be a power of two for
    the cidr value to be displayed
    """
    if not is_power_of_two(mask + 1):
        return 0
    mask += 1
    cidr = 33
    while mask:
        cidr -= 1
        mask >>= 1
    return cidr



#
# --------------------------------------------------------------------------------

def inet_ntoa(n):
    """
    Defina a local variant which exactly meets local needs
    """
    return "%s.%s.%s.%s" % ( \
            (n & 0xff000000) >> 24, \
            (n & 0x00ff0000) >> 16, \
            (n & 0x0000ff00) >> 8, \
            (n & 0x000000ff) )

#
# --------------------------------------------------------------------------------

def inet_aton(ip):
    """
    Return an integer containing the ip address passed in.
    Assumes the field is a quad-int
    """
    fields = ip.split('.')
    return (int(fields[0]) << 24) | \
           (int(fields[1]) << 16) | \
           (int(fields[2]) << 8) | \
           (int(fields[3]))

#
# --------------------------------------------------------------------------------

def ip_and_mask_ntoa(ip, mask):
    """
    The two values are displayed either as 'any' or as in cidr format
    or as a ip/mask depending on the value of the mask.
    (note the leading space)
    """
    if ip == '0.0.0.0' and mask == '255.255.255.255':
        return 'any '
    n_mask = inet_aton(mask)
    if is_power_of_two(n_mask + 1):
        return "%s/%d " % (ip, mask_to_cidr(n_mask))
    return "%s %s " % (ip, mask)


#
# --------------------------------------------------------------------------------

def ip_invert_netmask(value):
    split_bytes = value.split('.')
    return "%s.%s.%s.%s" % (255-int(split_bytes[0]),
                            255-int(split_bytes[1]),
                            255-int(split_bytes[2]),
                            255-int(split_bytes[3]))

#
# --------------------------------------------------------------------------------

def ip_and_neg_mask(ip, mask):
    """
    The two values are displayed either as 'any' or as in cidr format
    or as a ip/mask depending on the value of the mask.
    (note the leading space).  This is different from ip_and_mask_ntoa
    since the mask need to be printed an an inverted mask when the
    mask is displayed
    """
    if ip == '0.0.0.0' and mask == '255.255.255.255':
        return 'any '
    n_mask = inet_aton(mask)
    if is_power_of_two(n_mask + 1):
        cidr = mask_to_cidr(n_mask)
        if cidr:
            return "%s/%d " % (ip, mask_to_cidr(n_mask))
        return "%s " % ip
    return "%s %s " % (ip, ip_invert_netmask(mask))


#
# --------------------------------------------------------------------------------

SINGLEQUOTE_RE = re.compile(r"'")
DOUBLEQUOTE_RE = re.compile(r'"')
WHITESPACE_RE = re.compile(r"\s")

COMMAND_DPID_RE = re.compile(r'^(([A-Fa-f\d]){2}:?){7}[A-Fa-f\d]{2}$')
COMMAND_SEPARATORS_RE = re.compile(r'[>;|]') # XXX belongs in some global


def quote_string(value):
    """
    Return a quoted version of the string when there's imbedded
    spaces.  Worst case is when the string has both single and
    double quotes.
    """

    if SINGLEQUOTE_RE.search(value):
        if DOUBLEQUOTE_RE.search(value):
            new_value = []
            for c in value:
                if c == '"':
                    new_value.append("\\")
                new_value.append(c)
            return ''.join(new_value)
        else:
            return '"%s"' % value
    elif (DOUBLEQUOTE_RE.search(value) or
          WHITESPACE_RE.search(value) or
          COMMAND_SEPARATORS_RE.search(value)):
        return "'%s'" % value
    else:
        return value


#
# --------------------------------------------------------------------------------

def add_delim(strings_list, delim):
    """
    Add 'delim' to each string entry in the list, typically used to add a space
    to completion choices for entries which aren't prefixes.
    word mean a 
    
    """
    return [str(x) + delim for x in strings_list]


#
# --------------------------------------------------------------------------------

def convert_case(case, value):
    """
    Convert value to the requested case
    """
    if case == None:
        return value
    elif case == 'lower':
        return value.lower()
    elif case == 'upper':
        return value.upper()
    return value


TAIL_INT_RE = re.compile(r'^(.*[^0-9])(\d+)$')


#
# --------------------------------------------------------------------------------

def trailing_integer_cmp(x,y):
    """
    sorted() comparison function.

    Used when the two keys may possibly have trailing numbers,
    if these are compared using a typical sort, then the trailing
    numbers will be sorted alphabetically, not numerically.  This
    is most obvious when interfaces, for example Eth1, Eth10, Eth2
    are sorted.   Alphabetically the order is as already shown, but
    the desired sort order is Eth1, Eth2, Eth10
    """

    def last_digit(value):
        # only interested in sequences tailing a digit, where
        # the first charcter isn't a digit, used to sidestep tail_int re
        if len(value) >= 2:
            last_char = ord(value[-1])
            if last_char >= ord('0') and last_char <= ord('9'):
                first_char = ord(value[0])
                if first_char < ord('0') or first_char > ord('9'):
                    return True
        return False

    if type(x) == int and type(y) == int:
        return cmp(x,y)
    if last_digit(x) and last_digit(y):
        x_m = TAIL_INT_RE.match(x)
        y_m = TAIL_INT_RE.match(y)
        c = cmp(x_m.group(1), y_m.group(1))
        if c:
            return c
        c = cmp(int(x_m.group(2)), int(y_m.group(2)))
        if c:
            return c
    else:
        c = cmp(try_int(x), try_int(y))
        if c != 0:
            return c;

    return 0

COMPLETION_TAIL_INT_RE = re.compile(r'^([A-Za-z0-9-]*[^0-9])(\d+) $')

#
# --------------------------------------------------------------------------------

def completion_trailing_integer_cmp(x,y):
    """
    sorted() comparison function.

    This is used for completion values sorting.

    This function differs from trailing_integer_cmp, since for completions,
    the last character may be a space to indicate that the selection is
    complete.  If the last character is a space, the character ahead
    of it is used instead.
    """

    x_v = x
    if isinstance(x_v, tuple):
        x_v = x_v[0]
    if x[-1] == ' ':
        x_v = x[:-1]

    y_v = y
    if isinstance(y_v, tuple):
        y_v = y_v[0]
    if y[-1] == ' ':
        y_v = y[:-1]
    return trailing_integer_cmp(x_v, y_v)


#
# --------------------------------------------------------------------------------

ABERRANT_PLURAL_MAP = {
    'appendix': 'appendices',
    'barracks': 'barracks',
    'cactus': 'cacti',
    'child': 'children',
    'criterion': 'criteria',
    'deer': 'deer',
    'echo': 'echoes',
    'elf': 'elves',
    'embargo': 'embargoes',
    'focus': 'foci',
    'fungus': 'fungi',
    'goose': 'geese',
    'hero': 'heroes',
    'hoof': 'hooves',
    'index': 'indices',
    'knife': 'knives',
    'leaf': 'leaves',
    'life': 'lives',
    'man': 'men',
    'mouse': 'mice',
    'nucleus': 'nuclei',
    'person': 'people',
    'phenomenon': 'phenomena',
    'potato': 'potatoes',
    'self': 'selves',
    'syllabus': 'syllabi',
    'tomato': 'tomatoes',
    'torpedo': 'torpedoes',
    'veto': 'vetoes',
    'woman': 'women',
    }

VOWELS = set('aeiou')

def pluralize(singular):
    """Return plural form of given lowercase singular word (English only). Based on
    ActiveState recipe http://code.activestate.com/recipes/413172/
    
    >>> pluralize('')
    ''
    >>> pluralize('goose')
    'geese'
    >>> pluralize('dolly')
    'dollies'
    >>> pluralize('genius')
    'genii'
    >>> pluralize('jones')
    'joneses'
    >>> pluralize('pass')
    'passes'
    >>> pluralize('zero')
    'zeros'
    >>> pluralize('casino')
    'casinos'
    >>> pluralize('hero')
    'heroes'
    >>> pluralize('church')
    'churches'
    >>> pluralize('x')
    'xs'
    >>> pluralize('car')
    'cars'

    """
    if not singular:
        return ''
    plural = ABERRANT_PLURAL_MAP.get(singular)
    if plural:
        return plural
    root = singular
    try:
        if singular[-1] == 'y' and singular[-2] not in VOWELS:
            root = singular[:-1]
            suffix = 'ies'
        elif singular[-1] == 's':
            if singular[-2] in VOWELS:
                if singular[-3:] == 'ius':
                    root = singular[:-2]
                    suffix = 'i'
                else:
                    root = singular[:-1]
                    suffix = 'ses'
            else:
                suffix = 'es'
        elif singular[-2:] in ('ch', 'sh'):
            suffix = 'es'
        else:
            suffix = 's'
    except IndexError:
        suffix = 's'
    plural = root + suffix
    return plural
