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

import re
import os.path

from django.forms import ValidationError
from django.core.validators import RegexValidator, MinValueValidator, MaxValueValidator

validate_mac_address = RegexValidator('^(([A-Fa-f\d]){2}:?){5}[A-Fa-f\d]{2}$', 'MAC address is a 48-bit quantity, expressed in hex as AA:BB:CC:DD:EE:FF', 'invalid')
validate_dpid = RegexValidator('^([A-Fa-f\d]{2}:?){7}[A-Fa-f\d]{2}$', 'Switch DPID is a 64-bit quantity, expressed in hex as AA:BB:CC:DD:EE:FF:00:11', 'invalid')
validate_controller_id = RegexValidator('^[A-Fa-f\d]{8}-([A-Fa-f\d]{4}-){3}[A-Fa-f\d]{12}$', 'Controller ID is a 128-bit quantity, expressed in hex as aabbccdd-eeff-0011-2233-445566778899', 'invalid')

class UfwProtocolValditor(object):
    def __init__(self):
        pass
    
    def __call__(self, value):
        if not (value != "tcp" or value != "udp" or value != 'vrrp'):
            raise ValidationError("Protocol must be either 'tcp' or 'udp' or 'vrrp'")

octet = r'(?:0|[1-9][0-9]*)'
IP_RE = re.compile("^" + octet + '[.]' + octet
                   + '[.]' + octet + '[.]' + octet + "$")

class IpValidator(object):
    def __call__(self, value):
        if not IP_RE.match(value) or len([x for x in value.split('.') if int(x) < 256]) != 4:
            raise ValidationError("IP must be in dotted decimal format, 234.0.59.1")
            
class IpMaskValidator(object):
    def __call__(self, value):
        if not IP_RE.match(value):
            raise ValidationError("IP must be in dotted decimal format, 255.255.128.0")
        invalid_netmask = False
        invalid_inverse_netmask = False
        lookForZero = False
        for x in value.split('.'):
            xInt = int(x)
            if lookForZero:
                if xInt:
                    invalid_netmask = True
                    break  # go check inverse mask
            if xInt != 255:
                if xInt:
                    if xInt >= 128:
                        while (xInt % 2) == 0:
                            xInt = xInt >> 1
                        if xInt & (xInt + 1) == 0:
                            continue
                    invalid_netmask = True
                    break # go check inverse mask
                lookForZero = True

        lookForOne = False
        for x in value.split('.'):
            xInt = int(x)
            if lookForOne: # all bits should be 1 bits
                if (xInt != 255):
                    invalid_inverse_netmask = True
                    break
                else:
                    continue
            if xInt != 0:
                # check for contiguous 1-bits from LSb
                lookForOne = True
                if xInt == 255:
                    continue
                if xInt & (xInt + 1) != 0:
                    invalid_inverse_netmask = True
                    break
        if invalid_netmask and invalid_inverse_netmask: # no valid mask, then raise exception
            raise ValidationError("Invalid IP Mask, should be like 255.255.128.0 or 0.0.127.255")


class PortRangeSpecValidator(object):
    def __init__(self):
        self.RANGE_RE = re.compile(r'^([A-Za-z0-9-/\.:]*?)(\d+)\-(\d+)$')
        self.SINGLE_RE = re.compile(r'^([A-Za-z0-9-/\.:]+)$')
    def __call__(self, value):
        values = value.split(',')
        for v in values:
            m = self.RANGE_RE.match(v);
            if (m):
                startport = int(m.group(2))
                endport = int(m.group(3))
                if (startport >= endport):
                    raise ValidationError("Invalid range numerals: %d-%d" % (startport, endport))
            elif not self.SINGLE_RE.match(v):
                raise ValidationError("Must be a list of ports or ranges, such as 'A10-15,B25,C1-13'")

class VLANRangeSpecValidator(object):
    def __init__(self):
        self.RANGE_RE = re.compile(r'^([\d]+)\-([\d]+)$')
        self.SINGLE_RE = re.compile(r'^[\d]+$')

    def __call__(self, value):
        values = value.split(',')
        for v in values:
            m = self.RANGE_RE.match(v);
            if v == 'untagged':
                pass
            elif (m):
                if (int(m.group(1)) >= int(m.group(2))):
                    raise ValidationError("Invalid range numerals in {}: {} must be less than {}".format(v, m.group(1), m.group(2)))
                elif (int(m.group(1)) > 4095):
                    raise ValidationError("Invalid VLAN: {} must be in range 0-4095".format(m.group(1)))
                elif (int(m.group(2)) < 0):
                    raise ValidationError("Invalid VLAN: {} must be in range 0-4095".format(m.group(2)))
            elif not self.SINGLE_RE.match(v):
                raise ValidationError("Must be a list of VLANs or ranges,"
                                      " such as '5-20,45,4053,untagged'")
            elif int(v) > 4095 or int(v) < 0:
                raise ValidationError("Invalid VLAN: {} must be in range 0-4095".format(v))

class TagSpecValidator(object):
    def __init__(self):
        self.TAG_NAME_RE = re.compile(r'^([a-zA-Z0-9_-]+(?:\.?[a-zA-Z0-9_-]+)*)=+([a-zA-Z0-9_-]+)$')

    def __call__(self,value):
        values = value.split(',')
        for v in values:
            v = v.strip()
            if not self.TAG_NAME_RE.match(v):
                raise ValidationError("Invalid tag: " + 
                        '='.join(v.split('|')) +
                        "\nFormat: tag namespace.namel value1, " +
                        "where namespace may be empty or must be a-z, A-Z, 0-9, -, . or _, " + 
                        "name and value must be a-z, A-Z, 0-9, - or _")

class CidrValidator(object):
    def __init__(self, mask_required=True):
        self.mask_required = mask_required
        self.CIDR_RE = re.compile(r'^(\d{1,3}\.){3}\d{1,3}/\d{1,2}?$')

    def __call__(self, value):
        ip_validator = IpValidator()
        if self.CIDR_RE.match(value):
            ip, mask = value.split('/')
            ip_validator(ip)
            if int(mask) < 1 or int(mask) > 32:
                raise ValidationError("Mask should be between 1-32")
        else:
            if self.mask_required: # if mask was required and we didn't match above, problem!
                raise ValidationError("Must be in dotted decimal format with a mask between 1-32")
            else:
                ip_validator(value)
        
class EnumerationValidator(object):
    def __init__(self, enumerated_values, case_sensitive=False, message=None, code=None):
        self.case_sensitive = case_sensitive
        if case_sensitive:
            self.enumerated_values = enumerated_values
        else:
            self.enumerated_values = [s.lower() for s in enumerated_values]
        if not message:
            message = 'Invalid enumerated value'
        self.message = message
        if not code:
            code = 'invalid'
        self.code = code

    def __call__(self, value):
        if not self.case_sensitive:
            value = value.lower()
        if value not in self.enumerated_values:
            raise ValidationError(self.message, self.code)

class ChoicesValidator(EnumerationValidator):
    def __init__(self, choices, case_sensitive=False, message=None, code=None):
        enumerated_values = [choice[0] for choice in choices]
        EnumerationValidator.__init__(self, enumerated_values, case_sensitive, message, code)

class RangeValidator(object):
    def __init__(self, min, max):
        self.min_value_validator = MinValueValidator(min)
        self.max_value_validator = MaxValueValidator(max)

    def __call__(self, value):
        try:
            self.min_value_validator(int(value))
            self.max_value_validator(int(value))
        except ValidationError:
            raise ValidationError('Ensure this value is within the range (%d-%d)' %
                                  (self.min_value_validator.limit_value,
                                   self.max_value_validator.limit_value))

class ControllerAliaVsalidator(object):
    def __init__(self):
        self.CONTROLLER_ALIAS_RE = re.compile(r'^[a-zA-Z0-9_-]+$')

    def __call__(self, value):
        if not self.CONTROLLER_ALIAS_RE.match(value):
            raise ValidationError("controller alias name must ba a-z, 0-9, _ or _")

class SwitchAliasValidator(object):
    def __init__(self):
        self.SWITCH_ALIAS_RE = re.compile(r'^[a-zA-Z0-9_-]+$')

    def __call__(self, value):
        if not self.SWITCH_ALIAS_RE.match(value):
            raise ValidationError("switch alias name must ba a-z, 0-9, _ or _")

class PortAliasValidator(object):
    def __init__(self):
        self.PORT_ALIAS_RE = re.compile(r'^[a-zA-Z0-9_-]+$')

    def __call__(self, value):
        if not self.PORT_ALIAS_RE.match(value):
            raise ValidationError("port alias name must ba a-z, 0-9, _ or _")

class HostAliasValidator(object):
    def __init__(self):
        self.HOST_ALIAS_RE = re.compile(r'^[a-zA-Z0-9_-]+$')

    def __call__(self, value):
        if not self.HOST_ALIAS_RE.match(value):
            raise ValidationError("host alias name must ba a-z, 0-9, _ or _")

class AddressSpaceNameValidator(object):
    def __init__(self):
        self.ADDRESS_SPACE_NAME_RE = re.compile(r'^[a-zA-Z0-9_-]+$')

    def __call__(self,value):
        if not self.ADDRESS_SPACE_NAME_RE.match(value):
            raise ValidationError("address-space name must be a-z, A-Z, 0-9, - or _")

class TenantNameValidator(object):
    def __init__(self):
        self.TENANT_NAME_RE = re.compile(r'^[a-zA-Z0-9_-]+$')

    def __call__(self,value):
        if not self.TENANT_NAME_RE.match(value):
            raise ValidationError("tenant name must be a-z, A-Z, 0-9, - or _")

class GeneralNameValidator(object):
    def __init__(self):
        self.GENERAL_NAME_RE = re.compile(r'^[a-zA-Z0-9_-]+$')

    def __call__(self,value):
        if not self.GENERAL_NAME_RE.match(value):
            raise ValidationError("Name must be a-z, A-Z, 0-9, - or _")

class VnsNameValidator(object):
    def __init__(self):
        self.VNS_NAME_RE = re.compile(r'^[a-zA-Z0-9_-]+$')

    def __call__(self,value):
        if not self.VNS_NAME_RE.match(value):
            raise ValidationError("vns name must be a-z, A-Z, 0-9, - or _")

class VnsInterfaceNameValidator(object):
    def __init__(self):
        self.VNS_INTERFACE_RE = re.compile(r'^[a-zA-Z0-9_-]+$')
        self.PORT_RE = re.compile(r'^([A-Za-z0-9-]*?)(\d+)$')
        self.MAC_RE = re.compile(r'^(([A-Fa-f\d]){2}:?){5}[A-Fa-f\d]{2}$')

    def __call__(self,value):
        if not self.VNS_INTERFACE_RE.match(value):
            items = value.split('/')
            if len(items) == 2:
                if not self.PORT_RE.match(items[1]) and \
                    not self.MAC_RE.match(items[1]):
                        raise ValidationError("interface name after '/' must be either a port or a mac address")
            else:
                raise ValidationError("invalid syntax for interface name")

class VnsAclNameValidator(object):
    def __init__(self):
        self.VNS_ACL_NAME_RE = re.compile(r'[a-zA-Z0-9_-]+$')

    def __call__(self,value):
        if not self.VNS_ACL_NAME_RE.match(value):
            raise ValidationError("acl name must be a-z, A-Z, 0-9, - or _")

class VnsAclEntryActionValidator(object):
    def __call__(self,value):
        if not "permit".startswith(value.lower()) and \
           not "deny".startswith(value.lower()):
            raise ValidationError("acl entry action must be 'permit' or 'deny'")

class VnsInterfaceAclInOutValidator(object):
    def __call__(self,value):
        if not "in".startswith(value.lower()) and \
           not "out".startswith(value.lower()):
            raise ValidationError("acl entry action must be 'permit' or 'deny'")

class VnsRuleNameValidator(object):
    def __init__(self):
        self.IF_NAME_RE = re.compile(r'^\d*$')

    def __call__(self, value):
        if not self.IF_NAME_RE.match(value):
            print value
            raise ValidationError("Invalid rule name, only digits allowed")

class TagNameValidator(object):
    def __init__(self):
        self.TAG_NAME_RE = re.compile(r'^[a-zA-Z0-9_-]+(?:\.?[a-zA-Z0-9_-]+)*(?:\|[a-zA-Z0-9_-]+)+$')

    def __call__(self,value):
        if not self.TAG_NAME_RE.match(value):
            raise ValidationError("Invalid tag: " + value + "\nFormat: tag namespace|namel|value1, " +
                        "where namespace may be empty or must be a-z, A-Z, 0-9, -, . or _, " + 
                        "name and value must be a-z, A-Z, 0-9, - or _")


class VnsArpModeValidator(object):
    def __init__(self):
        self.ARP_MODES = ['flood-if-unknown', 'always-flood', 'drop-if-unknown']

    def __call__(self,value):
        if not value in self.ARP_MODES:
            raise ValidationError("must be one of %s" % ', '.join(self.ARP_MODES))

class VnsDhcpModeValidator(object):
    def __init__(self):
        self.DHCP_MODES = ['flood-if-unknown', 'always-flood', 'static']

    def __call__(self,value):
        if not value in self.DHCP_MODES:
            raise ValidationError("must be one of %s" % ', '.join(self.DHCP_MODES))

class VnsBroadcastModeValidator(object):
    def __init__(self):
        self.BROADCAST_MODES = ['drop', 'always-flood', 'forward-to-known']

    def __call__(self,value):
        if not value in self.BROADCAST_MODES:
            raise ValidationError("must be one of %s" % ', '.join(self.BROADCAST_MODES))
        
class IntfNameValidator(object):
    def __init__(self):
        self.IntfName_RE = re.compile(r'^ethernet[0-9]')
        
    def __call__(self, value):
        if not self.IntfName_RE.match(value):
            raise ValidationError("Interface name must be Ethernet<num>")

class DomainNameValidator(object):
    def __init__(self):
        self.DomainName_RE = re.compile(r'^([a-zA-Z0-9-]+.?)+$')
    def __call__(self,value):
        if not self.DomainName_RE.match(value):
            raise ValidationError("Value must be a valid domain name")

class IpOrDomainNameValidator(object):
    def __init__(self):
        self.DomainName_RE = re.compile(r'^([a-zA-Z0-9-]+.?)+$')
        
    def __call__(self,value):
        if not IP_RE.match(value) and not self.DomainName_RE.match(value):
            raise ValidationError("Value must be a valid IP address or domain name")
        
class TimeZoneValidator(object):
    timezones = None
    def __call__(self, value):
        if not TimeZoneValidator.timezones:
            import pytz
            TimeZoneValidator.timezones = pytz.all_timezones
        if value not in TimeZoneValidator.timezones:
            raise ValidationError("Invalid time zone string")
        
class VCenterMgrIdsValidator(object):
    def __init__(self):
        self.VCenterMgrId_RE = re.compile(r'^[a-zA-Z]([a-zA-Z0-9_-])*')
    def __call__(self,value):
        if not self.VCenterMgrId_RE.match(value):
            raise ValidationError("Value must be a valid name, starts with a alphabet and can have alphabets, numbers, _ and -")
        
class VCenterObjNamesValidator(object):
    def __init__(self):
        self.VCenterObjName_RE = re.compile(r'(?!.*\|.*)')
    def __call__(self, value):
        if not self.VCenterObjName_RE.match(value):
            raise ValidationError("VCenter object names can contain any character except '|'")        
    
class FeatureValidator(object):
    """Validates that a specific feature has been installed

    We assume that a feature has been installed if the following file exists:
        {sdncon.SDN_ROOT}/feature/<feature-name>

    NOTE: The feature is enabled/disabled for use by updating the controller
    object, this validator just ensures that before we enable a feature, it
    has been actually installed.
    """
    def __init__(self, feature, featuredir=None):
        self.feature = feature
        if featuredir is None:
            featuredir = \
                    os.path.join(os.path.sep, 'opt', 'sdnplatform', 'feature')
        self.featurefile = os.path.join(featuredir, feature)

    def __call__(self, value):
        if value and not os.path.isfile(self.featurefile):
            raise ValidationError(
                    'Feature not installed, please install "%s"' %
                    self.feature)

class VlanStringValidator(object):
    def __call__(self, value):
        self.vlan = 0
        try:
            self.vlan = int(value)
            if (self.vlan < 1 or self.vlan > 4095):
                raise ValidationError("VLAN must be in the range of 1 to 4095 with 4095 as untagged")
        except ValueError:
            raise ValidationError(
             "VLAN must be in the range of 1 to 4095 with 4095 as untagged")


class SafeForPrimaryKeyValidator(object):
    """Validates that a string does not contain any character that would be 
    illegal for use in a primary key. Currently this is the pipe | symbol
    """

    def __call__(self, value):
        if "|" in value:
            raise ValidationError(
             "The pipe symbol '|' is not a valid character")
        

class IsRegexValidator(object):
    """Validates that a given string is a valid regular expression
    """

    def __call__(self, value):
        try:
            value = str(value)
            re.compile(value)
        except re.error as e:
            raise ValidationError(
              "Input is not a valid regular expression: %s", e)

