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

from django.db import models
from django.contrib.auth.models import User

from django.forms import ValidationError
from django.core.validators import MaxValueValidator
from sdncon.rest.validators import *

import sys # for sys.maxint
import time

#
# controller/models.py
# ------------------------------------------------------------
#
# model description of the tables used to configure and 
# view the controller state.
#
# these tables are used both by sdnplatform and the rest api.
# the cli uses the rest api to read and write these tables,
# while sdnplatform/sdnplatform has a more direct interface to the
# tables.
#
# for the cli, the names of the fields are described in the
# 'class Rest' section of each of the tables.
#
# for sdnplatform the names of fields have a relationship to their
# type for sdnplatform.   If a field is a foreign key, then an '_id'
# is appended to the name for the same field within sdnplatform.  This
# means if a field is promoted or demoted to/from a foreign key,
# changes need to be made in the sdnplatform code to use the updated
# name.
#
# Make sure to include the nested Rest class or else they won't be
# accessible using the REST API.

#
#
# ------------------------------------------------------------

def get_timestamp():
    """
    For us in models where the method of ordering the row values
    is based on the order of creation.  
    
    The field is exposed to the rest api so that the value can
    be queries, and so that the rest api may choose to order the
    row values using its own strategy by setting the field directly
    """
    return int(time.time()*1000000)

class Tunnel(models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------

    #
    # Unique name of the Tunnel
    #
    tunnel_id = models.CharField(
        primary_key  = True,
        verbose_name = 'Tunnel Id',
        help_text    = 'A unique Id for a Tunnel',
        validators   = [ TenantNameValidator() ],
        max_length   = id_max_length)
    
    path_seq = models.CharField(
        verbose_name = 'Path Sequence',
        help_text    = 'List of path identifiers: nodes or adjacencies',
        blank        = True,
        null         = True,
        max_length   = 500)

    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.tunnel_id
    
    def delete(self):
        super(Tunnel, self).delete()
    class Rest:
        NAME = 'tunnel-config'
        FIELD_INFO = (
            {'name': 'tunnel_id',    'rest_name': 'tunnel-id'},
            {'name': 'path_seq',     'rest_name': 'path-seq'},
            )

class Policy(models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------

    #
    # Unique name of the Tunnel
    #
    sr_policy_id = models.CharField(
        primary_key  = True,
        verbose_name = 'SR Policy Id',
        help_text    = 'A unique Id for a SR Policy',
        validators   = [ TenantNameValidator() ],
        max_length   = id_max_length)
    
    sr_policy_type = models.CharField(
        verbose_name = 'SR Policy Type',
        help_text    = 'Type of SR Policy',
        validators   = [ TenantNameValidator() ],
        max_length   = id_max_length)
    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.sr_policy_id
    
    def delete(self):
        super(Policy, self).delete()

    class Rest:
        NAME = 'policy-config'
        FIELD_INFO = (
            {'name': 'sr_policy_id',    'rest_name': 'policy-id'},
            {'name': 'sr_policy_type',    'rest_name': 'policy-type'},
            )

#
# ------------------------------------------------------------

class Feature(models.Model):

    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key=True,
        verbose_name='feature',
        default='feature',
        max_length=16)

    netvirt_feature = models.BooleanField(
        verbose_name='Network virtualization Feature Enabled',
        help_text='Enable network virtualization feature by setting to true',
        default=True
        )

    static_flow_pusher_feature = models.BooleanField(
        verbose_name='Static Flow Pusher Feature Enabled',
        help_text='Enable Static Flow Pusher feature by setting to true',
        default=True
        )
    
    performance_monitor_feature = models.BooleanField(
        verbose_name='Performance Monitor',
        help_text="Enable performance monitoring feature by setting to true",
        default=False
        )
    
    #
    # end fields ----------------------------------------
    
    def __unicode__(self):
        return "%s" % (self.id,)

    def validate_unique(self, exclude = None):
        if self.id != 'feature':
            raise ValidationError("Only single feature record exists")

    class Rest:
        NAME = 'feature'
        FIELD_INFO = (
            {'name': 'netvirt_feature',                'rest_name': 'netvirt-feature'},
            {'name': 'static_flow_pusher_feature', 'rest_name': 'static-flow-pusher-feature'},
            {'name': 'performance_monitor_feature','rest_name': 'performance-monitor-feature'},
            )

"""
#
# ------------------------------------------------------------

class GlobalConfig(models.Model):
    #
    # fields ----------------------------------------
    
    name = models.CharField(
        primary_key=True,
        verbose_name='Global Config Name',
        help_text="Unique name for the global config; it's a singleton, "
                  "so, by convention, the name should always be \"global\"",
        default='global',
        max_length=16)
    
    cluster_name = models.CharField(
        verbose_name='Cluster Name',
        help_text='Name for the cluster',
        default='SDNCluster',
        max_length=256)

    cluster_number = models.IntegerField(
        verbose_name='Cluster Number',
        help_text='Small integral (1-255) identifier for the cluster',
        default=0)

    ha_enabled = models.BooleanField(
        verbose_name='High Availability (HA) Enabled',
        help_text='Is high availability (HA) enabled',
        default=False)

    #
    # end fields ----------------------------------------

    class Rest:
        NAME = 'global-config'
        FIELD_INFO = (
            {'name': 'cluster_name',    'rest_name': 'cluster-name'},
            {'name': 'cluster_number',  'rest_name': 'cluster-number'},
            {'name': 'ha_enabled',      'rest_name': 'ha-enabled'},
            )
    
#
# ------------------------------------------------------------

class TopologyConfig(models.Model):
    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key=True,
        verbose_name='topology',
        default='topology',
        max_length=16)


    autoportfast = models.BooleanField(
        verbose_name='Auto PortFast',
        help_text='Suppress sending LLDPs on fast ports.',
        default=True
        )

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return "%s" % (self.id,)

    def validate_unique(self, exclude = None):
        if self.id != 'topology':
            raise ValidationError("Only single topology record exists")

    class Rest:
        NAME = 'topology-config'
        FIELD_INFO = (
        )


#
# ------------------------------------------------------------

class ForwardingConfig(models.Model):
    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key=True,
        verbose_name='forwarding',
        default='forwarding',
        max_length=16)

    access_priority = models.IntegerField(
        verbose_name='Access Flow Priority',
        help_text='Priority for flows created by forwarding on access switches',
        validators=[ RangeValidator(0,2**15-1) ],
        default=10)

    core_priority = models.IntegerField(
        verbose_name='Core Flow Priority',
        help_text='Priority for flows created by forwarding on core switches',
        validators=[ RangeValidator(0,2**15-1) ],
        default=20)

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return "%s" % (self.id,)

    def validate_unique(self, exclude = None):
        if self.id != 'forwarding':
            raise ValidationError(
                "Only a single forwarding configuration record is allowed"
        )

    class Rest:
        NAME = 'forwarding-config'
        FIELD_INFO = (
            {'name': 'access_priority',    'rest_name': 'access-priority'},
            {'name': 'core_priority',      'rest_name': 'core-priority'},
        )
"""
#
# ------------------------------------------------------------
class Controller(models.Model):
    #
    # fields ----------------------------------------
    #
    # Note: This model should only contain config state for the controller VM,
    # not any operational state.
    #
    # Note: The convention used to be that the controller ID was
    # the IP address and port that the OpenFlow controller was listening
    # on. In practice SDNPlatform listened on all interfaces and it's logic
    # for determining its real IP address was unreliable, so it was
    # changed/simplified to just always use "localhost" for the 
    # IP address. So the controller ID was pretty much always
    # "localhost:6633". The use of "controller here caused some
    # confusion about whether "controller" meant the
    # OpenFlow controller (i.e. SDNPlatform/SDNPlatform) vs. the controller VM.
    # In the old controller model most/all of the settings concerned the
    # OpenFlow controller not the VM>
    # Most of the settings we now want to associate with the controller are
    # controller VM settings (e.g. IP address, DNS, time zone) and not
    # OpenFlow controller settings. So we're repurposing the Controller
    # model to be the controller VM config state and not the OpenFlow
    # controller discovered state (which was sort of broken to begin with).
    # Currently (as of 10/2011), since we only support single node operation
    # the controller ID is hard-coded to be localhost (probably should be
    # something different, e.g. "default", because localhost implies a
    # an interface which is really something separate), but eventually for
    # multi-node operation we'll need to have unique ids for each controller
    # node in the cluster. The easiest way would be to have the user enter
    # something at first time config. Or possibly we could do something
    # with GUIDs. Needs more thought...
    id = models.CharField(
        primary_key=True,
        verbose_name='Controller ID',
        help_text='Unique identifier string for the controller node',
        validators=[ validate_controller_id ],
        max_length=256)
    
    status = models.CharField(
        verbose_name='Status',
        help_text='cluster status of node',
        max_length=256,
        default='Provisioned',
        )

    domain_lookups_enabled = models.BooleanField(
        verbose_name='Domain Lookups Enabled',
        help_text='If domain lookups are enabled (default is True)',
        default=True
        )

    domain_name = models.CharField(
        verbose_name='Domain Name',
        help_text='Domain name of the network',
        max_length=256,
        validators=[ DomainNameValidator() ],
        default='',
        blank=True)

    default_gateway = models.CharField(
        verbose_name='Default Gateway',
        help_text='Default gateway',
        max_length=256,
        validators=[ IpValidator() ],
        default='',
        blank=True)
    
    ntp_server = models.CharField(
        verbose_name='NTP Server',
        help_text='NTP server',
        max_length=256,
        validators=[ IpOrDomainNameValidator() ],
        default='',
        blank=True,
        null=True)

    time_zone = models.CharField(
        verbose_name='Time Zone',
        help_text='Time zone (e.g. America/Los_Angeles)',
        max_length=256,
        validators=[ TimeZoneValidator() ],
        default='UTC')

    logging_enabled = models.BooleanField(
        verbose_name='Logging Enabled',
        help_text='Logging enabled',
        default=False
    )
    
    logging_server = models.CharField(
        verbose_name='Logging Server',
        help_text='Logging server',
        max_length=256,
        validators=[ IpOrDomainNameValidator() ],
        default='',
        blank=True,
        null=True)
    
    logging_level = models.CharField(
        verbose_name='Logging Level',
        help_text='Logging level',
        max_length=16,
        validators=[ EnumerationValidator(('emerg', 'alert', 'crit', 'err',
            'warning', 'notice', 'info', 'debug', '0', '1', '2', '3', '4',
            '5', '6', '7'))],
        default='notice'
        )
    
    # IOS let's you specify the domain name(s) of the network in two
    # ways. You can specify a single domain name with the "ip domain name <domain-name>"
    # command. You can specify multiple domain names with multiple
    # "ip domain list <domain-name>" commands. The domain names specified with
    # "ip domain list" commands take precedence over the domain name specified with
    # the "ip domain name" command, so the single "ip domain name" is only
    # used if the domain name list is unspecified/empty/null. Kind of messy, but
    # if we want to emulate IOS behavior I think we'll need to represent that in the
    # model. But to simplify things for now we'll just support the single domain name.
    
    #domain_name_list = models.TextField(
    #    verbose_name='Domain Name List',
    #    help_text='List of domain names for the network, one per line',
    #    null=True
    #    )

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return "%s" % (self.id,)

    class Rest:
        NAME = 'controller-node'
        FIELD_INFO = (
            {'name': 'domain_lookups_enabled',  'rest_name': 'domain-lookups-enabled'},
            {'name': 'domain_name',             'rest_name': 'domain-name'},
            {'name': 'default_gateway',         'rest_name': 'default-gateway'},
            {'name': 'ntp_server',              'rest_name': 'ntp-server'},
            {'name': 'time_zone',               'rest_name': 'time-zone'},
            {'name': 'logging_enabled',         'rest_name': 'logging-enabled'},
            {'name': 'logging_server',          'rest_name': 'logging-server'},
            {'name': 'logging_level',           'rest_name': 'logging-level'},
            )

"""
#
# ------------------------------------------------------------

class ControllerAlias(models.Model):
    
    #
    # fields ----------------------------------------

    alias = models.CharField(
        primary_key=True,
        max_length=255,
        help_text = "alias for controller",
        verbose_name='alias',
        validators=[SwitchAliasValidator()])
    
    controller = models.ForeignKey(
        Controller,
        verbose_name='Controller')

    #
    # end fields ----------------------------------------

    class Rest:
        NAME = 'controller-alias'
#
# ------------------------------------------------------------

class ControllerInterface(models.Model):
    
    #
    # fields ----------------------------------------
    
    controller = models.ForeignKey(
        Controller,
        verbose_name="Controller ID")
    
    type = models.CharField(
        verbose_name='Interface Type',
        help_text='Type of interface (e.g. "Ethernet")',
        max_length=256,
        default='Ethernet'
        )
    
    number = models.IntegerField(
        verbose_name="Interface Number",
        help_text='Number of interface (non-negative integer)',
        default=0)
    
    mode = models.CharField(
        verbose_name='Mode',
        help_text='Mode of configuring IP address ("dhcp" or "static")',
        validators=[ EnumerationValidator(('dhcp', 'static'))],
        max_length=32,
        default='static')
    
    ip = models.CharField(
        verbose_name='IP Address',
        help_text='IP Address for interface',
        validators=[ IpValidator() ],
        max_length=15,
        default='',
        blank=True)
    
    netmask = models.CharField(
        verbose_name='Netmask',
        help_text='Netmask',
        validators=[ IpValidator() ],
        max_length=15,
        default='',
        blank=True)
    
    # provide a link between the underlying interface layer
    # and this model's 'index number' to help manage discovered_ip
    mac = models.CharField(
        verbose_name="MAC Address",
        help_text="MAC Address",
        max_length=17, 
        validators = [validate_mac_address],
        blank=True,
        null=True)

    discovered_ip = models.CharField(
        verbose_name='Discovered IP Address',
        help_text='Discovered IP Address for interface',
        validators=[ IpValidator() ],
        max_length=15,
        default='',
        blank=True)
    

#    in_acl = models.ForeignKey(
#        ControllerAcl,
#        verbose_name = 'Controller input acl',
#        blank=True,
#        null=True)

#    out_acl = models.ForeignKey(
#        ControllerAcl,
#        verbose_name = 'Controller output acl',
#        blank=True,
#        null=True)
        
    #
    # end fields ----------------------------------------

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('controller', 'type', 'number')
    
    class Rest:
        NAME = 'controller-interface'
        FIELD_INFO = (
             {'name': 'discovered_ip', 'rest_name': 'discovered-ip'},
#            {'name': 'in_acl',        'rest_name': 'in-acl'},
#            {'name': 'out_acl',       'rest_name': 'out-acl'},
             )
        

#
# ------------------------------------------------------------

class FirewallRule(models.Model):

    #
    # fields ----------------------------------------
    
    interface = models.ForeignKey(
        ControllerInterface,
        verbose_name="Controller Interface")

    action = models.CharField(
        max_length=8,
        validators=[ EnumerationValidator(('allow', 'deny', 'reject'))],
        default='allow',
        blank=True)

    src_ip = models.CharField(
        verbose_name='Source IP',
        help_text='IP Address to allow traffic from',
        validators=[ IpValidator() ],
        max_length=15,
        default='',
        blank=True)
    
    vrrp_ip = models.CharField(
        verbose_name='Local IP',
        help_text='(Local) IP Address to allow traffic to',
        validators=[ IpValidator() ],
        max_length=15,
        default='',
        blank=True)
    
    port = models.IntegerField(
        verbose_name='Port Number',
        help_text='Port Number',
        validators=[ RangeValidator(0,2**16-1) ],
        default=0,
        blank=True)
    
    proto = models.CharField(
        verbose_name="ip proto",
        help_text="ip protocol (tcp, udp or vrrp)", #TODO validator
        validators=[ UfwProtocolValditor() ],
        max_length=4,
        default='',
        blank=True)
    
    #
    # end fields ----------------------------------------

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('interface', 'src_ip', 'vrrp_ip', 'port', 'proto')
        
    class Rest:
        NAME = 'firewall-rule'
        FIELD_INFO = (
             {'name': 'src_ip', 'rest_name': 'src-ip'},
             {'name': 'vrrp_ip', 'rest_name': 'vrrp-ip'},
        )

#
# ------------------------------------------------------------

class ControllerDomainNameServer(models.Model):
    
    controller = models.ForeignKey(
        Controller,
        verbose_name="Controller ID",
        default=None,
        null=True)
    
    ip = models.CharField(
        verbose_name='IP Address',
        help_text='IP Address of domain name server',
        validators=[ IpValidator() ],
        max_length=15,
        default='')

    timestamp = models.IntegerField(
        verbose_name='timestamp',
        help_text='Timestamp to determine order of domain name servers',
        default = get_timestamp,
        null=True,
        blank=True,
        )

    #
    # end fields ----------------------------------------

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('controller', 'ip')
        
    def validate_unique(self, exclude = None):
        try:
            exists = ControllerDomainNameServer.objects.get(controller = self.controller,
                                                        ip = self.ip)
            if exists.timestamp != self.timestamp:
                print 'SHAZAM', self.timestamp
                self.timestamp = exists.timestamp
        except:
            pass

    class Rest:
        NAME = 'controller-domain-name-server'
        FIELD_INFO = (
            )
"""
#
# ------------------------------------------------------------

class Switch(models.Model):
    switch_id_length = 23
    #
    # fields ----------------------------------------

    dpid = models.CharField(
        primary_key=True,
        verbose_name='Switch DPID',
        help_text='Switch DPID - 64-bit hex separated by :',
        max_length=switch_id_length, 
        validators=[ validate_dpid ])

    controller = models.ForeignKey(
        Controller,
        verbose_name='Controller ID',
        blank=True,                           
        null=True)
    
    socket_address = models.CharField(
        verbose_name='Socket Address',
        help_text='Socket address used for connection to controller',
        max_length=64,
        blank=True,
        null=True)
    
    ip = models.CharField(
        verbose_name='IP Address',
        help_text='IP Address used for connection from controller',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)

    active = models.BooleanField(
        verbose_name='Active',
        help_text='Switch is active (i.e. connected to a controller)',
        default=False)

    connected_since = models.DateTimeField(
        verbose_name='Last Connect Time',
        help_text='Time when the switch connected to the controller',
        blank=True,
        null=True)
    
    dp_desc = models.CharField(
        verbose_name='Description',
        max_length=256,
        blank=True,
        null=True)

    hw_desc = models.CharField(
        verbose_name='Hardware Description',
        max_length=256,
        blank=True,
        null=True)

    sw_desc = models.CharField(
        verbose_name='Software Description',
        max_length=256,
        blank=True,
        null=True)

    serial_num = models.CharField(
        verbose_name='Serial Number',
        max_length=32,
        blank=True,
        null=True)

    capabilities = models.IntegerField(
        verbose_name='Capabilities',
        help_text='Openflow switch capabilities',
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)

    tunneling_supported = models.BooleanField(
        default=False,
        )

    buffers = models.IntegerField(
        verbose_name='Max Packets',
        help_text='Maximum number of packets buffered by the switch',
        validators=[ RangeValidator(0,2**32-1) ],
        blank=True,
        null=True)

    tables = models.IntegerField(
        verbose_name='Max Tables',
        help_text='Number of tables supported by the switch',
        validators=[ RangeValidator(0,2**8-1) ],
        blank=True,
        null=True)

    actions = models.IntegerField(
        verbose_name='Actions',
        help_text='Actions supported by the switch',
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)
    
    #
    # end fields ----------------------------------------

    # LOOK! we should have an origin field to distinguish
    # between user-specified switches and 'discovered' switches

    def __unicode__(self):
        return "%s" % self.dpid

    class Rest:
        NAME = 'switch'
        FIELD_INFO = (
            {'name': 'socket_address',    'rest_name': 'socket-address'},
            {'name': 'ip',                'rest_name': 'ip-address'},
            {'name': 'connected_since',   'rest_name': 'connected-since'},
            {'name': 'dp_desc',           'rest_name': 'dp-desc'},
            {'name': 'hw_desc',           'rest_name': 'hw-desc'},
            {'name': 'sw_desc',           'rest_name': 'sw-desc'},
            {'name': 'serial_num',        'rest_name': 'serial-num'},
            {'name': 'tunneling_supported', 'rest_name': 'tunnel-supported'},
            )
#
# ------------------------------------------------------------
# SwitchConfig
#  Any 'configured' (non-discovered) state associated with
#  a switch.
#
#  tunnel_termination; when enabled, tunnels are constructed
#   to any other tunnel_termination switch, building a mesh
#   of open-flow enabled switches.  Typicall Used in virtualized
#   environments, where openflow switches are not intended to
#   exist in the path.
#

class SwitchConfig(models.Model):
    switch_id_length = 23
    #
    # fields ----------------------------------------

    dpid = models.CharField(
        primary_key=True,
        verbose_name='Switch DPID',
        help_text='Switch DPID - 64-bit hex separated by :',
        max_length=switch_id_length, 
        validators=[ validate_dpid ])

    core_switch = models.BooleanField(
        default=False,
        help_text='Identify core switches'
        )

    tunnel_termination = models.CharField(
        verbose_name='Tunnel Termination',
        help_text='Tunnel Termination ("enabled" "disabled" "default")',
        validators=[ EnumerationValidator(('enabled', 'disabled', 'default'))],
        default = 'default',
        max_length=16)

    #
    # end fields ----------------------------------------


    def validate_unique(self, exclude = None):
        self.dpid = self.dpid.lower()

    class Rest:
        NAME = 'switch-config'
        FIELD_INFO = (
            {'name': 'tunnel_termination', 'rest_name': 'tunnel-termination'},
            {'name': 'core_switch',       'rest_name': 'core-switch'},
            )

#
# ------------------------------------------------------------

class SwitchAlias(models.Model):
    
    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key=True,
        max_length=255,
        help_text = "alias for switch",
        verbose_name='alias',
        validators=[SwitchAliasValidator()])
    
    switch = models.ForeignKey(
        SwitchConfig,
        verbose_name='Switch DPID')

    #
    # end fields ----------------------------------------

    class Rest:
        NAME = 'switch-alias'

"""
#
# ------------------------------------------------------------

class Port(models.Model):
    #
    # fields ----------------------------------------
    #
    # This table isn't intended to be updated via the rest api,
    #  sdnplatform writes the table to describe a switch.
    #
    # The 'number' in the port model is the openflow port number,
    #  which is a value used in setting up flow entries.  This is
    #  not an interface name;  the 'interface name' is the 'name'
    #  field below.  This table provides a mapping from the switch
    #  dpid and port number to an 'interface name'.
    #
    # Since interface names can be configured on demand by the
    #  switch, for example to name a tunnel, its not easy to
    #  "guess" interface names without the switch reporting 
    #  what interface names exist.  This leads to difficulty
    #  in preconfiguring any associations with <switch, interface name>
    #

    # Unique identifier for the port
    # combination of the switch DPID and the port number
    id = models.CharField(
        primary_key=True,
        verbose_name='Port ID',
        help_text = '#|switch|number',
        max_length=48)
    
    switch = models.ForeignKey(
        Switch,
        verbose_name='Switch DPID')

    number = models.IntegerField(
        verbose_name='OF #',
        help_text="Port open flow number",
        validators=[ RangeValidator(0,2**16-1) ])
    
    hardware_address = models.CharField(
        verbose_name="MAC Address",
        help_text="Port MAC Address",
        max_length=17, 
        validators = [validate_mac_address],
        blank=True,
        null=True)
    
    name = models.CharField(
        verbose_name='Name',
        help_text="Port name",
        max_length=32,
        blank=True,
        null=True)

    config = models.IntegerField(
        verbose_name='Configuration',
        help_text='Configuration Flags',
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)

    state = models.IntegerField(
        verbose_name="State",
        help_text="State Flags",
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)
    
    current_features = models.IntegerField(
        verbose_name='Current',
        help_text='Current Features',
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)

    advertised_features = models.IntegerField(
        verbose_name='Advertised',
        help_text='Advertised Features',
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)

    supported_features = models.IntegerField(
        verbose_name='Supported',
        help_text='Supported Features',
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)

    peer_features = models.IntegerField(
        verbose_name='Peer',
        help_text='Peer Features',
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return "%s" % (self.id,)

    class Rest:
        NAME = 'port'
        FIELD_INFO = (
            {'name': 'hardware_address',     'rest_name': 'hardware-address'},
            {'name': 'current_features',     'rest_name': 'current-features'},
            {'name': 'advertised_features',  'rest_name': 'advertised-features'},
            {'name': 'supported_features',   'rest_name': 'supported-features'},
            {'name': 'peer_features',        'rest_name': 'peer-features'},
            )

#
# ------------------------------------------------------------

class PortAlias(models.Model):
    
    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key=True,
        max_length=255,
        help_text = "alias for port",
        verbose_name='alias',
        validators=[PortAliasValidator()])
    
    port = models.ForeignKey(
        Port,
        help_text = "foreign key for port alias",
        verbose_name='Port ID')

    #
    # end fields ----------------------------------------

    class Rest:
        NAME = 'port-alias'
#
# ------------------------------------------------------------

class SwitchInterfaceConfig(models.Model):
    if_name_len = 32
    #
    # fields ----------------------------------------

    switch = models.ForeignKey(
        SwitchConfig,
        verbose_name='Switch')

    if_name = models.CharField(
        verbose_name='If Name',
        validators=[ SafeForPrimaryKeyValidator() ],
        max_length=32)

    mode = models.CharField(
        verbose_name='Mode',
        help_text='Interface Mode ("external", "default")',
        validators=[ EnumerationValidator(('external','default'))],
        max_length=32,
        default='default')
    
    broadcast = models.BooleanField(
        default=False,
        verbose_name='Broadcast',
        help_text='True when interfaces is an uplink to a legacy net',
        )

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return "%s" % (self.id)

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('switch', 'if_name')

    def validate_unique(self, exclude = None):
        self.broadcast = False
        if self.mode == 'external':
            self.broadcast = True

    class Rest:
        NAME = 'switch-interface-config'
        FIELD_INFO = (
            # By using 'name' here, the 'name' from the port is identified
            #  in the complete-from-another procedure
            {'name': 'if_name',             'rest_name': 'name'},
            )


#
# ------------------------------------------------------------

class SwitchInterfaceAlias(models.Model):
    switch_interface_alias_length = 255
    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key = True,
        verbose_name = 'Switch Interface Alias',
        help_text = 'alias',
        max_length = switch_interface_alias_length,
        validators=[HostAliasValidator()])

    switch_interface = models.ForeignKey(
        SwitchInterfaceConfig,
        verbose_name='Switch Interface',
        )

    #
    # end fields ----------------------------------------

    class Rest:
        NAME = 'switch-interface-alias'
        FIELD_INFO = (
            {'name': 'switch_interface', 'rest_name': 'switch-interface'},
        )
#
# ------------------------------------------------------------

class StaticFlowTableEntry(models.Model):
    #
    # fields ----------------------------------------

    name = models.CharField(
        primary_key=True,
        verbose_name='Name', 
        max_length=80)

    switch = models.ForeignKey(
        SwitchConfig, 
        verbose_name="Switch DPID")

    active = models.BooleanField(
        default=False)
    
    # LOOK! we should hide the timeout values for static flow entry
    # definition as they are overwritten (unless we allow these to
    # overwrite the default that static flow pusher uses)
    idle_timeout = models.IntegerField(
        verbose_name="Idle Timeout", 
        help_text="Expire flow after this many seconds of inactivity - default: 60",
        default=60,
        validators=[ RangeValidator(0, 2**16-1) ])
                                       
    hard_timeout = models.IntegerField(
        verbose_name="Hard Timeout",
        help_text="Seconds to expire flow, regardless of activity - default: 0",
        default=0,
        validators=[ RangeValidator(0, 2**16-1) ])

    priority = models.IntegerField(
        verbose_name="Priority", 
        help_text="Priority of the flow entry",
        default=32768,
        validators=[ RangeValidator(0, 2**16-1) ])

    cookie = models.IntegerField(
        verbose_name="Cookie",
        default=0) # 64-bit

    #
    # match fields
    #

    # LOOK! Need a file of python openflow constants 

    # LOOK! we should hide this also or at least
    #       for static flow entries say it is ignored
    wildcards = models.IntegerField(
        verbose_name="Wildcards",
        default=0,
        validators=[ RangeValidator(0,2**32-1) ])

    in_port = models.IntegerField(
            verbose_name="Ingress Port", 
            blank=True, 
            help_text="Open flow port number of ingress port",
            null=True, 
            validators = [ RangeValidator(0, 2**16-1) ] ) 

    dl_src = models.CharField(
            verbose_name="Src MAC", 
            help_text="This is a 48-bit quantity specified in xx:xx:xx:xx:xx:xx format", 
            max_length=17, 
            blank=True, 
            null=True,
            validators = [ validate_mac_address ] )

    dl_dst = models.CharField(
            verbose_name="Dst MAC", 
            help_text="Destination MAC address in the frames",
            max_length=17, 
            blank=True, 
            null=True, 
            validators = [ validate_mac_address ] )

    dl_vlan = models.IntegerField(
            verbose_name="VLAN ID",
            help_text="VLAN ID in the frames",
            blank=True, 
            null=True, 
            validators = [ RangeValidator(0, 2**12-1) ]) 

    dl_vlan_pcp = models.IntegerField(
            verbose_name="VLAN Priority", 
            help_text="VLAN ID in the frames",
            blank=True, 
            null=True, 
            validators = [ RangeValidator(0, 2**3-1) ]) 

    dl_type = models.IntegerField(
            verbose_name="Ether Type", 
            help_text="Ether(L3) type",
            blank=True, 
            null=True, 
            validators = [ RangeValidator(0, 2**16-1) ]) 

    nw_tos = models.IntegerField(
            verbose_name="TOS Bits",
            help_text="TOS bits in the frame",
            blank=True,
            null=True,
            validators = [ RangeValidator(0, 2**6-1) ]) # 6-bit DSCP value

    nw_proto = models.IntegerField(
            verbose_name="Protocol", 
            help_text="IP (L4) protocol in the packets",
            blank=True,
            null=True, 
            validators = [ RangeValidator(0, 2**8-1) ]) 

    nw_src = models.CharField(
            verbose_name="Src IP", 
            help_text="IP v4 source address in dotted decimal a.b.c.d w/ optional mask (ex: /24)", 
            max_length=18,
            validators = [ CidrValidator(mask_required=False) ],
            blank=True, 
            null=True)

    nw_dst = models.CharField(
            verbose_name="Dst IP", 
            help_text="IP v4 destination address in dotted decimal a.b.c.d w/ optional mask (ex: /24)", 
            validators=[ CidrValidator(mask_required=False) ],
            max_length=18,
            blank=True, 
            null=True )

    tp_src = models.IntegerField(
            verbose_name="Src Port", 
            help_text="Source (TCP/UDP) port",
            blank=True, 
            null=True,
            validators=[ RangeValidator(0, 2**16-1) ])

    tp_dst = models.IntegerField(
            verbose_name="Dst Port", 
            help_text="Destination (TCP/UDP) port",
            blank=True, 
            null=True, 
            validators=[ RangeValidator(0, 2**16-1) ])

    # LOOK! have to figure out how to expose actions in the CLI - this is ugly/brittle
    actions = models.CharField(
            verbose_name = "Actions",
            help_text="This is a comma-separated list of actions - a la dpctl", 
            max_length=1024, 
            blank=True, 
            null=True)

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.name


    class Rest:
        NAME = 'flow-entry'
        FIELD_INFO = (
            {'name': 'idle_timeout',    'rest_name': 'idle-timeout'},
            {'name': 'hard_timeout',    'rest_name': 'hard-timeout'},
            {'name': 'in_port',         'rest_name': 'ingress-port'},
            {'name': 'dl_src',          'rest_name': 'src-mac'},
            {'name': 'dl_dst',          'rest_name': 'dst-mac'},
            {'name': 'dl_vlan',         'rest_name': 'vlan-id'},
            {'name': 'dl_vlan_pcp',     'rest_name': 'vlan-priority'},
            {'name': 'dl_type',         'rest_name': 'ether-type'},
            {'name': 'nw_tos',          'rest_name': 'tos-bits'},
            {'name': 'nw_proto',        'rest_name': 'protocol'},
            {'name': 'nw_src',          'rest_name': 'src-ip'},
            {'name': 'nw_dst',          'rest_name': 'dst-ip'},
            {'name': 'tp_src',          'rest_name': 'src-port'},
            {'name': 'tp_dst',          'rest_name': 'dst-port'},
            {'name': 'actions',         'rest_name': 'actions'},
            )

#
# ------------------------------------------------------------

class Link(models.Model):
    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key=True,
        verbose_name='Link ID',
        max_length=64)
    
    src_switch = models.ForeignKey(
        Switch,
        verbose_name='Src Switch DPID',
        related_name='src_link_set')
    #src_switch_id = models.CharField(
    #    verbose_name='Src Switch ID',
    #    max_length=32)
    
    name = models.CharField(
        verbose_name='Name',
        help_text="Link name",
        max_length=32,
        blank=True,
        null=True)
    
    src_port = models.IntegerField(
        verbose_name="Src Port",
        validators=[ RangeValidator(0, 2**16-1) ])

    src_port_state = models.IntegerField(
        verbose_name="Src Port State",
        help_text="Source Port State Flags",
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)

    dst_switch = models.ForeignKey(
        Switch,
        verbose_name='Dst Switch DPID',
        related_name='dst_link_set')
    #dst_switch_id = models.CharField(
    #    verbose_name='Dst Switch ID',
    #    max_length=32)
   
    dst_port = models.IntegerField(
        verbose_name="Dst Port",
        help_text="Destination Port",
        validators=[ RangeValidator(0, 2**16-1) ])

    dst_port_state = models.IntegerField(
        verbose_name="Dst Port State",
        help_text="Destination Port State Flags",
        validators=[ RangeValidator(0,2**32-1) ],
        default=0)

    link_type = models.CharField(
                            verbose_name='Link Type',
                            help_text="Link type",
                            max_length=10,
                            blank=True,
                            null=True)
    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return "%s" % self.id

    class Rest:
        NAME = 'link'
        FIELD_INFO = (
            {'name': 'src_switch',      'rest_name': 'src-switch'},
            {'name': 'src_port',        'rest_name': 'src-port'},
            {'name': 'src_port_state',  'rest_name': 'src-port-state'},
            {'name': 'dst_switch',      'rest_name': 'dst-switch'},
            {'name': 'dst_port',        'rest_name': 'dst-port'},
            {'name': 'dst_port_state',  'rest_name': 'dst-port-state'},
            {'name': 'link_type',       'rest_name': 'link-type'}
            )
            
"""
"""
#
# ------------------------------------------------------------
# An address-space separation

class AddressSpace (models.Model):

    id_max_length = 64

    #
    # fields ----------------------------------------

    #
    # Unique name of the address-space
    #
    name = models.CharField(
        primary_key  = True,
        verbose_name = 'Address Space Name',
        help_text    = 'A unique name for an Address Space Seperation',
        validators   = [ AddressSpaceNameValidator() ],
        max_length   = id_max_length)

    #
    # Verbose description of this rule.
    #
    description = models.CharField(
        verbose_name = 'Description',
        help_text    = "Description of the address-space",
        max_length   = 128,
        blank        = True,
        null         = True)
   
    #
    # Whether the configuration is active ? By default, it is active
    # Used to disable the configuration without having to delete the entire
    # address-space configuration construct.
    #
    active = models.BooleanField(
        verbose_name = 'Active',
        help_text    = 'Is this Address Space active (default is True)',
        default      = True)

    #
    # Priority of this address-space during device matching when
    # compared to other address-spaces. Those at the same priority
    # are used in a determinisitc alphanetical order.
    #
    priority = models.IntegerField(
        verbose_name = 'Priority',
        help_text    = 'Priority for this Address Space ' +
                       '(higher numbers are higher priority)',
        default      = 1000,
        validators   = [ RangeValidator(0, 65535) ])

    #
    # Seperator tag of this address-space in the data plane, such as vlan id.
    #
    vlan_tag_on_egress = models.IntegerField(
        verbose_name = 'Egress VLAN tag',
        help_text    = 'Vlan Tag value used for this Address Space separation',
        default      = None,
        blank        = True,
        null         = True,
        validators   = [ RangeValidator(0, 4095) ])

    #
    # Origin of this configured item
    # 
    origin = models.CharField(
        verbose_name = "Origin",
        help_text    = "Values: cli, rest, other packages",
        max_length   = 64, # in future we might use SW GUIDs for this field
        blank        = True,
        null         = True)

    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.name

    class Rest:
        NAME       = 'address-space'
        FIELD_INFO = (
            {'name': 'vlan_tag_on_egress', 'rest_name':'vlan-tag-on-egress'},
        )

#
# ------------------------------------------------------------
# An identifier rule in address-space separation

class AddressSpaceIdentifierRule (models.Model):
    rule_max_length = 32 

    #
    # fields ----------------------------------------

    #
    # Create a reference to the enclosing address-space construct.
    #
    address_space = models.ForeignKey(
        AddressSpace,
        verbose_name = 'Address Space Identifier')

    #
    # Unique rule identifier name under this address-space.
    #
    rule = models.CharField(
        verbose_name = 'Address Space Rule Identifier',
        max_length   = rule_max_length)

    #
    # Verbose description of this rule.
    #
    description = models.CharField(
        verbose_name = 'Description',
        help_text    = "Description of rule",
        max_length   = 128,
        blank        = True,
        null         = True)

    #
    # Whether the configuration is active ? By default, it is active
    # Used to disable the configuration without having to delete the entire
    # address-space identifier-rule configuration construct.
    #
    active = models.BooleanField(
        verbose_name = 'Active',
        help_text    = 'If this interface is active (default is True)',
        default      = True)

    #
    # Priority of this address-space during device matching when
    # compared to other address-spaces. Those at the same priority
    # are used in a determinisitc alphanetical order.
    #
    priority = models.IntegerField(
        verbose_name = 'Priority',
        help_text    = 'Priority for this interface rule ' +
                       '(higher numbers are higher priority)',
        default      = 32768,
        validators   = [ RangeValidator(0, 65535) ])

    #
    # DPID of the Switch which sent the packet
    #
    switch = models.CharField(
        verbose_name = 'Switch DPID',
        max_length   = Switch.switch_id_length,
        help_text    = 'Switch DPID or switch alias',
        validators   = [ validate_dpid ],
        null         = True,
        blank        = True)

    #
    # Range of ports in which the packet came from
    #
    ports = models.CharField(
        verbose_name = "Port Range Spec",
        help_text    = 'Port range (e.g. C12 or B1,A22-25)',
        max_length   = 256,
        validators   = [ PortRangeSpecValidator() ],
        blank        = True,
        null         = True)
    
    #
    # Range of VLAN tags
    #
    vlans = models.CharField(
        verbose_name = "VLAN Range Spec",
        help_text    = "VLAN(s) (e.g. 5 or 5-10,4010-4050)",
        max_length   = 256,
        validators   = [ VLANRangeSpecValidator() ],
        blank        = True,
        null         = True)

    #
    # NameValue pair of tags
    #
    tag = models.CharField(
        verbose_name = "Tag Spec",
        help_text    = "Tag values (e.g. namespace.tagname=value)",
        max_length   = 256,
        validators   = [ TagSpecValidator() ],
        blank        = True,
        null         = True)

    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('address_space', 'rule')

    class Rest:
        NAME = 'address-space-identifier-rule'
        FIELD_INFO = (
            {'name': 'description',     'rest_name': 'description'},
            {'name': 'address_space',   'rest_name': 'address-space'},
            {'name': 'active',          'rest_name': 'active'},
            {'name': 'priority',        'rest_name': 'priority'},
            {'name': 'switch',          'rest_name': 'switch'},
            {'name': 'ports',           'rest_name': 'ports'},
            {'name': 'vlans',           'rest_name': 'vlans'},
            {'name': 'tag',             'rest_name': 'tag'},
            )
#
# ------------------------------------------------------------
"""
class HostConfig(models.Model):
    host_id_length = 17
    #
    # fields ----------------------------------------

    #address_space = models.ForeignKey(
    #    AddressSpace,
    #    verbose_name = "Address space name")

    mac = models.CharField(
        verbose_name="MAC Address",
        max_length=host_id_length, 
        validators = [validate_mac_address])

    vlan = models.CharField(
        verbose_name='VLAN',
        help_text='VLAN Associated with host',
        max_length=4,
        validators=[RangeValidator(1, 4095)],
        blank=True,
        default='')

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        self.mac = self.mac.lower()
        #return "%s::%s" % (self.addressSpace, self.mac)
        return "%s" % (self.addressSpace)

    class CassandraSettings:
        #COMPOUND_KEY_FIELDS = ('address_space', 'vlan', 'mac')
        COMPOUND_KEY_FIELDS = ('vlan', 'mac')

    def validate_unique(self, exclude = None):
        # Invoke the default validator; error out if the vns already exists
        super(HostConfig, self).validate_unique(exclude)
        #if self.vlan and str(self.address_space) != 'default': 
        #    raise ValidationError('host: vlan configured for '
        #                          'address-space other than "default" %s' % self.address_space)

    class Rest:
        NAME = 'host-config'
        FIELD_INFO = (
            #{'name': 'address_space', 'rest_name': 'address-space'},
            )
"""
#
# ------------------------------------------------------------

class HostSecurityIpAddress(models.Model):
    host = models.ForeignKey(
        HostConfig,
        verbose_name='Host ID')

    ip = models.CharField(
        verbose_name='IP Address',
        help_text='IP Address used to associate with host',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('host', 'ip')

    class Rest:
        NAME = 'host-security-ip-address'
        FIELD_INFO = (
            {'name': 'ip', 'rest_name': 'ip-address'},
            )


#
# ------------------------------------------------------------

class HostSecurityAttachmentPoint(models.Model):
    host = models.ForeignKey(
        HostConfig,
        verbose_name='Host ID')

    dpid = models.CharField(
        verbose_name = 'Switch DPID',
        max_length   = Switch.switch_id_length,
        help_text    = 'Switch DPID or switch alias',
        validators   = [ validate_dpid ],
        null         = True,
        blank        = True)

    if_name_regex = models.CharField(
        verbose_name='If Name Regex',
        help_text='Interface name regular expression',
        max_length=64,
        validators = [SafeForPrimaryKeyValidator(), IsRegexValidator()],
        blank = True,
        null = False,
        )


    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('host', 'dpid', 'if_name_regex')

    class Rest:
        NAME = 'host-security-attachment-point'
        FIELD_INFO = (
            {'name': 'if_name_regex', 'rest_name': 'if-name-regex'},
            )

#
# ------------------------------------------------------------

class HostAlias(models.Model):
    host_alias_length = 255
    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key = True,
        verbose_name = 'Host Alias',
        help_text = 'alias',
        max_length = host_alias_length,
        validators=[HostAliasValidator()])

    host = models.ForeignKey(
        HostConfig,
        verbose_name='Host ID')

    #
    # end fields ----------------------------------------

    class Rest:
        NAME = 'host-alias'

class VlanConfig(models.Model):
    #
    # fields ----------------------------------------    
    vlan = models.IntegerField(
        primary_key = True,                       
        verbose_name='VLAN',
        help_text='VLAN Number',
        validators=[RangeValidator(0, 4095)],
        )
    
    #
    # end fields ----------------------------------------
    
    def __unicode__(self):
        if self.vlan:
            return "%s vlan %s" % (self.vlan)
        else:
            return "%s vlan %s" % (self.vlan)
    
    class Rest:
        NAME = 'vlan-config'

#
# ------------------------------------------------------------
# A Static ARP table separation

class StaticArp (models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------
    ip = models.CharField(
        primary_key=True,
        verbose_name='IP Address',
        validators=[ IpValidator() ],
        max_length=15)
    mac = models.CharField(
        verbose_name="MAC Address",
        max_length=17, 
        validators = [validate_mac_address]) 
    #
    # Origin of this configured item
    # 
    origin = models.CharField(
        verbose_name = "Origin",
        help_text    = "Values: cli, rest, other packages",
        max_length   = 64, # in future we might use SW GUIDs for this field
        blank        = True,
        null         = True)

    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.id
    
    class Rest:
        NAME = 'static-arp'
        


#
# ------------------------------------------------------------
# A Tenant separation

class Tenant (models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------

    #
    # Unique name of the tenant
    #
    name = models.CharField(
        primary_key  = True,
        verbose_name = 'Tenant Name',
        help_text    = 'A unique name for an Tenant',
        validators   = [ TenantNameValidator() ],
        max_length   = id_max_length)

    #
    # Verbose description of this tenant.
    #
    description = models.CharField(
        verbose_name = 'Description',
        help_text    = "Description of the tenant",
        max_length   = 128,
        blank        = True,
        null         = True)
   
    #
    # Whether the configuration is active ? By default, it is active
    # Used to disable the configuration without having to delete the entire
    # tenant configuration construct.
    #
    active = models.BooleanField(
        verbose_name = 'Active',
        help_text    = 'Is this Tenant active (default is True)',
        default      = True)

    #
    # Origin of this configured item
    # 
    origin = models.CharField(
        verbose_name = "Origin",
        help_text    = "Values: cli, rest, other packages",
        max_length   = 64, # in future we might use SW GUIDs for this field
        blank        = True,
        null         = True)

    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.name
    
    def delete(self):
        if self.name=='default' or self.name =='system' or self.name =='external':
            raise ValidationError("Default/External/System Tenant can't be deleted")
        super(Tenant, self).delete()
    class Rest:
        NAME = 'tenant'
        

#
# ------------------------------------------------------------
# A virtual router separation

class VirtualRouter (models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------
    vrname = models.CharField(
        verbose_name = 'Virtual Router Name',
        help_text    = 'A unique name for a virtual router',
        validators   = [ GeneralNameValidator() ],
        max_length   = id_max_length
        )
    
    tenant = models.ForeignKey(
        Tenant,
        verbose_name='Tenant Name',
        )
    description = models.CharField(
        verbose_name = 'Description',
        help_text    = "Description of the virtual router",
        max_length   = 128,
        blank        = True,
        null         = True,
        )
   
    #
    # Origin of this configured item
    # 
    origin = models.CharField(
        verbose_name = "Origin",
        help_text    = "Values: cli, rest, other packages",
        max_length   = 64, # in future we might use SW GUIDs for this field
        blank        = True,
        null         = True)

    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.id
    
    def validate_unique(self, exclude = None):
        # in fat tire, only one router per tenant can be defined. this can be removed later.
        error=False
        try:
            exists = VirtualRouter.objects.get(tenant = self.tenant)
            if exists.vrname !=self.vrname:
                error=True
        except:
            pass
        if error:
            raise ValidationError(" Virtual router %s has been defined for tenant %s, only one virtual router per tenant supported" % (exists.vrname,self.tenant))
    
    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('tenant', 'vrname')

    class Rest:
        NAME = 'virtualrouter'

#
# ------------------------------------------------------------
# A virtual network segment

class VNS(models.Model):
    id_max_length = 64
    #
    # fields ----------------------------------------
    vnsname = models.CharField(
        verbose_name='VNS ID',
        help_text='A unique name for a Virtual Network Segment',
        validators=[GeneralNameValidator()],
        max_length=id_max_length)
    tenant=models.ForeignKey(
        Tenant,
        verbose_name='Tenant ID',
        default='default')
    #
    # Verbose description of this rule.
    #
    description = models.CharField(
        verbose_name = 'Description',
        help_text    = "Description of the VNS",
        max_length   = 128,
        blank        = True,
        null         = True)

    #
    # Reference to the address-space item. By default, we 
    # implicitly use 'default' if this is not explicitly provided.
    #
    vns_address_space = models.ForeignKey(
        AddressSpace,
        verbose_name='Address Space Association',
        blank=True,
        null=True)

    active = models.BooleanField(
        verbose_name='Active',
        help_text='If this VNS is active (default is True)',
        default=True)

    priority = models.IntegerField(
        verbose_name='Priority',
        help_text='Priority for this VNS (higher numbers are higher priority)',
        default = 1000,
        validators=[RangeValidator(0, 65535)])
 
    origin = models.CharField(
        verbose_name = "The origin/creator interface for this VNS",
        help_text="Values: cli, rest, other packages",
        max_length=64, # in future we might use SW GUIDs for this field
        blank=True,
        null=True)

    arp_mode = models.CharField(
        verbose_name = "ARP Manager Config Mode",
        help_text="Values: always-flood, flood-if-unknown, drop-if-unknown", 
        max_length=32, 
        validators=[VnsArpModeValidator()],
        default='flood-if-unknown')

    dhcp_mode = models.CharField(
        verbose_name = "DHCP Manager Config Mode",
        help_text = "Values: always-flood, flood-if-unknown, static",
        max_length = 20,
        validators=[VnsDhcpModeValidator()],
        default='flood-if-unknown')
    
    dhcp_ip = models.CharField(
        verbose_name='DHCP IP Address',
        help_text='IP Address of DHCP Server',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)

    broadcast = models.CharField(
        verbose_name = "Broadcast (non ARP/DHCP) Config Mode",
        help_text = "Values: always-flood, forward-to-known, drop",
        max_length = 20,
        validators=[VnsBroadcastModeValidator()],
        default='forward-to-known')

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    def delete(self):
        #default VNS can't be deleted
        if self.id=='default|default':
            raise ValidationError("Default VNS can't be deleted")
        #while address space still exist, address space default vns can't be deleted
        #for fat tire, relationship between address space and tenant are unclear yet, the following part may need revisit
        suffix = '-default'
        if self.vnsname.endswith(suffix):
            print self.vnsname
            address_space_name = self.vnsname[:-len(suffix)]
            error=False
            try:
                self.vns_address_space = AddressSpace.objects.get(name = address_space_name)
                error=True
            except Exception, e:
                pass
            if error:
                raise ValidationError('vns %s is the default VNS of address space: %s, can not be deleted ' %
                                         (self.vnsname,address_space_name))
        super(VNS, self).delete()
    # manage a magic association between vns names and 
    # address space for vns's which end in -default
    def validate_unique(self, exclude = None):
        # Invoke the default validator; error out if the vns already exists
        #for fat tire, relationship between address space and tenant are unclear yet, the following part may need revisit
        super(VNS, self).validate_unique(exclude)
        suffix = '-default'
        if not 'vns_address_space' in exclude:
            if self.vns_address_space:
                if self.vnsname.endswith(suffix):
                    if str(self.vns_address_space) != self.vnsname[:-len(suffix)]:
                        raise ValidationError('vns names %s ending in -default '
                                'must have address_space names with the same prefix: %s '
                                % (self.vnsname, self.vns_address_space))
            elif self.vnsname.endswith(suffix):
                address_space_name = self.vnsname[:-len(suffix)]
                try:
                    self.vns_address_space = AddressSpace.objects.get(name = address_space_name)
                except Exception, e:
                    print e
                    if self.vns_address_space == None:
                        raise ValidationError('vns %s has no matching address-space %s ' %
                                             (self.vnsname, address_space_name))

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('tenant', 'vnsname')

    class Rest:
        NAME = 'vns-definition'
        FIELD_INFO = (
            {'name': 'vns_address_space', 'rest_name': 'address-space'},
            {'name': 'arp_mode',          'rest_name': 'arp-mode'},
            {'name': 'dhcp_mode',         'rest_name': 'dhcp-mode'},
            {'name': 'dhcp_ip',           'rest_name': 'dhcp-ip'},
            )
    
#
# ------------------------------------------------------------
# An interface rule on a VNS

class VNSInterfaceRule(models.Model):
    rule_max_length = 32 

    #
    # fields ----------------------------------------

    vns = models.ForeignKey(
        VNS,
        verbose_name='VNS ID')

    rule = models.CharField(
        verbose_name='VNS Rule ID',
        max_length=rule_max_length)

    description = models.CharField(
        verbose_name='Description',
        help_text="Description of rule",
        max_length=128,
        blank=True,
        null=True)

    vlan_tag_on_egress = models.BooleanField(
        verbose_name='Egress Vlan Tagging',
        help_text='Tag with VLAN at egress point (default is False)',
        default=False)

    allow_multiple = models.BooleanField(
        verbose_name='Allow Multiple',
        help_text='If this interface allows hosts to be on multiple VNS (default is False)',
        default=False)

    active = models.BooleanField(
        verbose_name='Active',
        help_text='If this interface is active (default is True)',
        default=True)

    priority = models.IntegerField(
        verbose_name='Priority',
        help_text='Priority for this interface rule (higher numbers are higher priority)',
        default = 32768,
        validators=[RangeValidator(0, 65535)])

    mac = models.CharField(
        verbose_name="MAC Address",
        help_text='MAC Address or host alias',
        max_length=17, 
        validators = [validate_mac_address],
        blank=True,
        null=True)

    ip_subnet = models.CharField(
        verbose_name="IP Subnet",
        help_text='IP address or subnet (e.g. 192.168.1.1 or 192.168.1.0/24)',
        max_length=31,
        validators = [CidrValidator(False)],
        blank=True,
        null=True)

    switch = models.CharField(
        verbose_name='Switch DPID',
        max_length= Switch.switch_id_length,
        help_text='Switch DPID or switch alias',
        validators=[ validate_dpid ],
        null=True,
        blank=True)

    ports = models.CharField(
        verbose_name="Port Range Spec",
        help_text='Port range (e.g. C12 or B1,A22-25)',
        max_length=256,
        validators = [PortRangeSpecValidator()],
        blank=True,
        null=True)

    vlans = models.CharField(
        verbose_name="VLAN Range Spec",
        help_text="VLAN(s) (e.g. 5 or 5-10,4010-4050)",
        max_length=256,
        validators = [VLANRangeSpecValidator()],
        blank=True,
        null=True)

    tags = models.CharField(
        verbose_name="Tag Spec",
        help_text="Tag values (e.g. namespace.tagname=value)",
        max_length=256,
        validators = [TagSpecValidator()],
        blank=True,
        null=True)


    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('vns', 'rule')

    class Rest:
        NAME = 'vns-interface-rule'
        FIELD_INFO = (
            {'name': 'description',        'rest_name': 'description'},
            {'name': 'allow_multiple',     'rest_name': 'allow-multiple'},
            {'name': 'active',             'rest_name': 'active'},
            {'name': 'priority',           'rest_name': 'priority'},
            {'name': 'mac',                'rest_name': 'mac'},
            {'name': 'ip_subnet',          'rest_name': 'ip-subnet'},
            {'name': 'switch',             'rest_name': 'switch'},
            {'name': 'ports',              'rest_name': 'ports'},
            {'name': 'vlans',              'rest_name': 'vlans'},
            {'name': 'tags',               'rest_name': 'tags'},
            {'name': 'vlan_tag_on_egress', 'rest_name': 'vlan-tag-on-egress'},            
            )

#
# ------------------------------------------------------------

class VNSInterfaceConfig(models.Model):
    name_max_length = 32
    #
    # fields ----------------------------------------

    vns = models.ForeignKey(
        VNS,
        verbose_name='VNS ID')

    interface = models.CharField(
        verbose_name='VNS Interface Name',
        max_length=name_max_length,
        validators = [VnsInterfaceNameValidator()])

    rule = models.ForeignKey(
        VNSInterfaceRule,
        verbose_name='VNS Rule ID',
        blank=True,
        null=True)

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('vns', 'interface')

    class Rest:
        NAME = 'vns-interface-config'
        FIELD_INFO = (
            {'name': 'rule',  'rest_name': 'rule'},
            )


#
# ------------------------------------------------------------

class VNSAcl(models.Model):
    name_max_length=32
    #
    # fields ----------------------------------------

    vns = models.ForeignKey(
        VNS,
        verbose_name='VNS ID')

    name = models.CharField(
        help_text='Acl Name',
        validators=[VnsAclNameValidator()],
        max_length=name_max_length)

    priority = models.IntegerField(
        verbose_name='Priority',
        help_text='Priority for this ACL (higher numbers are higher priority)',
        default = 32768,
        validators=[RangeValidator(0, 65535)])

    description = models.CharField(
        verbose_name='Description',
        help_text="Description of the ACL",
        max_length=128,
        blank=True,
        null=True)

    #
    # end fields ----------------------------------------

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('vns', 'name')

    def __unicode__(self):
        return self.id

    class Rest:
        NAME = 'vns-access-list'
        FIELD_INFO = (
            {'name': 'name',         'rest_name': 'name'},
            {'name': 'priority',     'rest_name': 'priority'},
            {'name': 'description',  'rest_name': 'description'},
            )

#
# ------------------------------------------------------------

class VNSAclEntry(models.Model):
    #
    # fields ----------------------------------------

    rule = models.CharField(
        help_text='Rule ID',
        validators=[VnsRuleNameValidator()],
        max_length=15)

    vns_acl = models.ForeignKey(
        VNSAcl,
        verbose_name='VNS Acl name')

    action = models.CharField(
        verbose_name='permit or deny',
        help_text="'permit' or 'deny'",
        max_length=16,
        validators=[ VnsAclEntryActionValidator() ])

    type = models.CharField(
        verbose_name='mac/ip/<0-255>/udp/tcp/icmp',
        help_text="ACLtype either mac or ip or udp or tcp or icmp or ip-protocol-type",
        max_length=16)

    src_ip = models.CharField(
        verbose_name='Source IP',
        help_text='Source IP Address to match',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)

    src_ip_mask = models.CharField(
        verbose_name='Source IP Mask',
        help_text='Mask to match source IP',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)

    src_tp_port_op = models.CharField(
        verbose_name='Source Port comparison op',
        help_text='Compare with tcp/udp port eq/neq/any',
        max_length=5,
        blank=True,
        null=True)
    
    src_tp_port = models.IntegerField(
        verbose_name='Source UDP/TCP Port',
        help_text='Source port value to compare',
        validators=[RangeValidator(0, 65535)],
        blank=True,
        null=True)
   
    dst_ip = models.CharField(
        verbose_name='Destination IP',
        help_text='Destination IP Address to match',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)

    dst_ip_mask = models.CharField(
        verbose_name='Destination IP Mask',
        help_text='Mask to match destination IP',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)

    dst_tp_port_op = models.CharField(
        verbose_name='Destination Port comparison op',
        help_text='Compare with tcp/udp port eq/neq/any',
        max_length=3,
        blank=True,
        null=True)
    
    dst_tp_port = models.IntegerField(
        verbose_name='Destination UDP/TCP Port',
        help_text='Destination port value to compare',
        validators=[RangeValidator(0, 65535)],
        blank=True,
        null=True)

    icmp_type = models.IntegerField(
        verbose_name='ICMP Type',
        help_text='Matching ICMP type icmp (blank matches all)',
        validators=[RangeValidator(0, 255)],
        blank=True,
        null=True)

    src_mac = models.CharField(
        verbose_name="Source MAC Address",
        help_text="Colon separated hex string (blank matches all)",
        max_length=17, 
        validators = [validate_mac_address],
        blank=True,
        null=True)
    
    dst_mac = models.CharField(
        verbose_name="Destination MAC Address",
        help_text="Colon separated hex string (blank matches all)",
        max_length=17, 
        validators = [validate_mac_address],
        blank=True,
        null=True)
    
    ether_type = models.IntegerField(
        verbose_name='Ethernet Packet Type',
        help_text='Standard ether type (blank matches all)',
        validators=[RangeValidator(1536, 65535)],
        blank=True,
        null=True)

    vlan = models.IntegerField(
        verbose_name="VLAN ID",
        help_text='Standard ether type (blank matches all)',
        blank=True, 
        null=True, 
        validators = [ RangeValidator(0, 4095) ]) 

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('vns_acl', 'rule')

    def validate_unique(self, exclude = None):
        #
        # there are three types of entries:
        # - mac based rules
        # - ip based rules
        # - tcp/udp based rules
        # 
        # verify that for each rules, unexpected fields are not
        #  populated

        if self.type == 'mac':
            if self.src_ip or self.src_ip_mask:
                raise ValidationError("vns-access-list-entry mac rule:"
                                      " src-ip/src-ip-mask specified"
                                      "(ought to be null)")
            if self.dst_ip or self.dst_ip_mask:
                raise ValidationError("vns-access-list-entry mac rule:"
                                      " dst-ip/dst-ip-mask specified "
                                      "(ought to be null)")
            if self.src_tp_port_op or self.src_tp_port:
                raise ValidationError("vns-access-list-entry mac rule:"
                                      " src-tp-port-op/src-to-port specified "
                                      "(ought to be null)")
            if self.dst_tp_port_op or self.dst_tp_port:
                raise ValidationError("vns-access-list-entry mac rule:"
                                      " dst-tp-port-op/dst-to-port specified "
                                      "(ought to be null)")
            if self.icmp_type:
                raise ValidationError("vns-access-list-entry mac rule:"
                                      " icmp_type specified "
                                      "(ought to be null)")
        elif self.type == 'ip' or re.match('[\d]+', self.type) or \
            self.type == 'icmp':
            if (self.src_tp_port_op != None or self.src_tp_port != None) and \
                ((self.src_tp_port_op == None) or (self.src_tp_port == None)):
                raise ValidationError("vns-access-list-entry ip rule:"
                                      " src-tp-port-op/src-to-port specified "
                                      "(both or neither)")
            if (self.dst_tp_port_op != None or self.dst_tp_port != None) and \
                ((self.dst_tp_port_op == None) or (self.dst_tp_port == None)):
                raise ValidationError("vns-access-list-entry ip rule:"
                                      " dst-tp-port-op/dst-to-port specified "
                                      "(both or neither)")
            if self.src_mac or self.dst_mac:
                raise ValidationError("vns-access-list-entry ip rule:"
                                      " src_mac/dst_mac specified "
                                      "(ought to be null)")
            if self.ether_type or self.vlan:
                raise ValidationError("vns-access-list-entry ip rule:"
                                      " ether-type/vlan specified "
                                      "(ought to be null)")

        elif self.type == 'tcp' or self.type == 'udp':
            if self.src_mac or self.dst_mac:
                raise ValidationError("vns-access-list-entry ip rule:"
                                      " src_mac/dst_mac specified "
                                      "(ought to be null)")
            if self.ether_type or self.vlan:
                raise ValidationError("vns-access-list-entry ip rule:"
                                      " ether-type/vlan specified "
                                      "(ought to be null)")


    class Rest:
        NAME = 'vns-access-list-entry'
        FIELD_INFO = (
            {'name': 'vns_acl',        'rest_name': 'vns-access-list'},
            {'name': 'src_ip',         'rest_name': 'src-ip'},
            {'name': 'src_ip_mask',    'rest_name': 'src-ip-mask'},
            {'name': 'dst_ip',         'rest_name': 'dst-ip'},
            {'name': 'dst_ip_mask',    'rest_name': 'dst-ip-mask'},
            {'name': 'src_tp_port_op', 'rest_name': 'src-tp-port-op'},
            {'name': 'src_tp_port',    'rest_name': 'src-tp-port'},
            {'name': 'dst_tp_port_op', 'rest_name': 'dst-tp-port-op'},
            {'name': 'dst_tp_port',    'rest_name': 'dst-tp-port'},
            {'name': 'icmp_type',      'rest_name': 'icmp-type'},
            {'name': 'src_mac',        'rest_name': 'src-mac'},
            {'name': 'dst_mac',        'rest_name': 'dst-mac'},
            {'name': 'ether_type',     'rest_name': 'ether-type'},
            )

#
# ------------------------------------------------------------

class VNSInterfaceAcl(models.Model):
    in_out_length = 4

    #
    # fields ----------------------------------------

    vns_acl = models.ForeignKey(
        VNSAcl,
        verbose_name='VNS Acl name')

    vns_interface = models.ForeignKey(
        VNSInterfaceConfig,
        verbose_name='VNS Interface ID',
        help_text='[vns id]|[interface long name]')

    in_out = models.CharField(
        verbose_name='in/out',
        help_text='Match on packet input or output',
        validators=[VnsInterfaceAclInOutValidator()],
        max_length=in_out_length,
        )

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('vns_interface', 'vns_acl', 'in_out')

    def validate_unique(self, exclude = None):
        acl_parts = str(self.vns_acl).split('|')
        intf_parts = str(self.vns_interface).split('|')
        # validate two vns parts
        if acl_parts[0] != intf_parts[0]:
            raise ValidationError("acl's vns %s doen't match interface vns %s" %
                                  (acl_parts[0], intf_parts[0]))
        error = False
        try:
            exists = VNSInterfaceAcl.objects.get(vns_interface=self.vns_interface, in_out=self.in_out)
            if exists:
                if exists.vns_acl != self.vns_acl:
                    error = True
        except:
            pass
        if error:
            raise ValidationError("Interface %s already has an ACL in the %s direction, only one ACL per direction allowed" % (self.vns_interface, self.in_out))

    class Rest:
        NAME = 'vns-interface-access-list'
        FIELD_INFO = (
            {'name': 'vns_acl',       'rest_name': 'vns-access-list'},
            {'name': 'vns_interface', 'rest_name': 'vns-interface'},
            {'name': 'in_out',        'rest_name': 'in-out'},
            )

#
# ------------------------------------------------------------
# A virtual router interface separation

class VirtualRouterInterface (models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------
    virtual_router = models.ForeignKey(
        VirtualRouter,
        verbose_name='Virtual Router ID')
    #
    # Unique name of the interface
    #
    vriname = models.CharField(
        verbose_name = 'Interface Name',
        help_text    = 'A unique name for a virtual router interface',
        validators   = [ GeneralNameValidator() ],
        max_length   = id_max_length)
    #
    # Origin of this configured item
    # 
    origin = models.CharField(
        verbose_name = "Origin",
        help_text    = "Values: cli, rest, other packages",
        max_length   = 64, # in future we might use SW GUIDs for this field
        blank        = True,
        null         = True)
    #
    # Whether the configuration is active ? By default, it is active
    # Used to disable the configuration without having to delete the entire
    # interface configuration construct.
    #
    active = models.BooleanField(
        verbose_name = 'Active',
        help_text    = 'Is this interface active (default is True)',
        default      = True)
    vns_connected = models.ForeignKey(
        VNS,
        verbose_name='VNS connected to',
        blank       =True,
        null        =True)
    router_connected = models.ForeignKey(
        VirtualRouter,
        related_name='router_connected',
        verbose_name='Virtual Router connected to',
        blank       =True,
        null        =True)
    
    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.id
    
    def validate_unique(self, exclude = None):
        def is_set(value):
            if value != None and value != '':
                return True

        # for vns connection, verify that only the VNSs under the same tenant can be connected
        error=False
        if not 'vns_connected' in exclude:
            if is_set(self.vns_connected):
                tenant_vns_parts = str(self.vns_connected).split('|')
                tenant_router_parts = str(self.virtual_router).split('|')
                if tenant_vns_parts[0] != tenant_router_parts[0]:
                    raise ValidationError(" VNS %s belongs to tenant %s, doesn't match virtual router tenant %s" %
                                      (tenant_vns_parts[1],tenant_vns_parts[0], tenant_router_parts[0]))
                    # verify there can only be one connection for one VNS
                try:
                    exists = VirtualRouterInterface.objects.get(virtual_router = self.virtual_router, vns_connected=self.vns_connected)
                    if exists:
                        if exists.vriname!=self.vriname:
                            error=True
                except:
                    pass
                if error:
                    raise ValidationError(" VNS %s has been connected, multiple connections is not allowed" % self.vns_connected)
        error = False    
        # for router connection, verify that the same virtual router as myself can't be connected        
        if not 'router_connected' in exclude:
            if is_set(self.router_connected):
                tenant_router_parts = str(self.router_connected).split('|')
                tenant_myrouter_parts = str(self.virtual_router).split('|')
                if tenant_router_parts[0] == tenant_myrouter_parts[0]:
                    raise ValidationError(" Local loop conncetion is not allowed.")
            # verify there can only be one connection for one virtual router
                try:
                    exists = VirtualRouterInterface.objects.get(virtual_router = self.virtual_router,router_connected=self.router_connected)
                    if exists:
                        if exists.vriname!=self.vriname:
                            error=True
                except:
                    pass
                if error:
                    raise ValidationError(" Virtual Router %s has been connected, multiple connections is not allowed" % self.router_connected)

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('virtual_router', 'vriname')

    class Rest:
        NAME = 'virtualrouter-interface'
        FIELD_INFO = (
                      {'name': 'vns_connected',           'rest_name': 'vns-connected'},
                      {'name': 'router_connected',        'rest_name': 'router-connected'},
                      {'name': 'virtual_router',          'rest_name': 'virtual-router'},
                      
            )

#
# ------------------------------------------------------------
# A virtual router interface address pool separation

class VRInterfaceIpAddressPool (models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------
    virtual_router_interface = models.ForeignKey(
        VirtualRouterInterface,
        verbose_name='Virtual Router Interface ID')
    #
    # Origin of this configured item
    # 
    origin = models.CharField(
        verbose_name = "Origin",
        help_text    = "Values: cli, rest, other packages",
        max_length   = 64,
        blank        = True,
        null         = True)
    ip_address = models.CharField(
        verbose_name='Source IP',
        help_text='Interface IP Address',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)
    subnet_mask = models.CharField(
        verbose_name='Subnet IP Mask',
        validators=[ IpMaskValidator() ],
        max_length=15,
        blank=True,
        null=True)
    
    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.id
    
    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('virtual_router_interface', 'ip_address')

    class Rest:
        NAME = 'interface-address-pool'
        FIELD_INFO = (
                      {'name': 'virtual_router_interface',           'rest_name': 'virtual-router-interface'},
                      {'name': 'ip_address',                         'rest_name': 'ip-address'},
                      {'name': 'subnet_mask',                        'rest_name': 'subnet-mask'}, 
            )


#
# ------------------------------------------------------------

class VirtualRouterGWPool (models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------
    virtual_router = models.ForeignKey(
        VirtualRouter,
        verbose_name='Virtual Router ID')
    #
    # Unique name of the gateway pool
    #
    vrgwname = models.CharField(
        verbose_name = 'Gateway Pool Name',
        help_text    = 'A unique name for a virtual router gateway pool',
        validators   = [ GeneralNameValidator() ],
        max_length   = id_max_length)
    
    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.id
    
    def validate_unique(self, exclude = None):
        def is_set(value):
            if value != None and value != '':
                return True

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('virtual_router', 'vrgwname')

    class Rest:
        NAME = 'virtualrouter-gwpool'
        FIELD_INFO = (
                      {'name': 'virtual_router',          'rest_name': 'virtual-router'},
            )

#
# ------------------------------------------------------------
# A virtual router gateway address pool separation

class VRGatewayIpAddressPool (models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------
    virtual_router_gwpool = models.ForeignKey(
        VirtualRouterGWPool,
        verbose_name='Virtual Router Gateway Pool ID')
    ip_address = models.CharField(
        verbose_name='Gateway IP',
        help_text='Gateway IP Address',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)
    
    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.id
    
    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('virtual_router_gwpool', 'ip_address')

    class Rest:
        NAME = 'gateway-address-pool'
        FIELD_INFO = (
                      {'name': 'virtual_router_gwpool', 'rest_name': 'virtual-router-gwpool'},
                      {'name': 'ip_address', 'rest_name': 'ip-address'},
            )

class VirtualRoutingRule (models.Model):

    id_max_length = 64
    #
    # fields ----------------------------------------
    virtual_router = models.ForeignKey(
        VirtualRouter,
        verbose_name='Virtual Router ID')
    #
    # Origin of this configured item
    # 
    origin = models.CharField(
        verbose_name = "Origin",
        help_text    = "Values: cli, rest, other packages",
        max_length   = 64,
        blank        = True,
        null         = True)
    src_host = models.ForeignKey(
        HostConfig,
        verbose_name='source Host ID',
        help_text='Source Host ID to match',
        blank       =True,
        null        =True)
    src_tenant = models.ForeignKey(
        Tenant,
        verbose_name='source tenant ID',
        help_text='Source tenant ID to match',
        blank       =True,
        null        =True)
    src_vns = models.ForeignKey(
        VNS,
        verbose_name='source VNS ID',
        help_text='Source VNS ID to match',
        blank       =True,
        null        =True)
    src_ip = models.CharField(
        verbose_name='Source IP',
        help_text='Source IP Address to match',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)
    src_ip_mask = models.CharField(
        verbose_name='Source IP Mask',
        help_text='Mask to match source IP',
        validators=[ IpMaskValidator() ],
        max_length=15,
        blank=True,
        null=True)
    dst_host = models.ForeignKey(
        HostConfig,
        verbose_name='Destination Host ID',
        help_text='Destination Host ID to match',
        related_name='dest_host',
        blank       =True,
        null        =True)
    dst_tenant = models.ForeignKey(
        Tenant,
        verbose_name='Destination tenant ID',
        related_name='dest_tenant',
        blank       =True,
        null        =True)
    dst_vns = models.ForeignKey(
        VNS,
        verbose_name='Destination VNS ID',
        related_name='dest_vns',
        blank       =True,
        null        =True)
    dst_ip = models.CharField(
        verbose_name='Destination IP',
        help_text='Destination IP Address to match',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)
    dst_ip_mask = models.CharField(
        verbose_name='Destination IP Mask',
        help_text='Mask to match destination IP',
        validators=[ IpMaskValidator() ],
        max_length=15,
        blank=True,
        null=True)
    outgoing_intf = models.ForeignKey(
        VirtualRouterInterface,
        verbose_name='Outgoing Interface',
        blank       =True,
        null        =True)
    gateway_pool = models.ForeignKey(
        VirtualRouterGWPool,
        verbose_name='Gateway pool',
        blank       =True,
        null        =True)
    nh_ip = models.CharField(
        verbose_name='Next Hop IP',
        help_text='Next Hop IP Address',
        validators=[ IpValidator() ],
        max_length=15,
        blank=True,
        null=True)
    action = models.CharField(
        verbose_name='permit or deny',
        help_text="'permit' or 'deny'",
        default='permit',
        max_length=16,
        validators=[ VnsAclEntryActionValidator() ])
    #
    # end fields ----------------------------------------

    def __unicode__ (self):
        return self.id
    
    def validate_unique(self, exclude = None):
        def is_set(value):
            if value != None and value != '':
                return True
    #verify the outgoing interface can only be on the local virtual router interface
        if not 'outgoing_intf' in exclude:
            if is_set(self.outgoing_intf):
                router_parts = str(self.outgoing_intf).split('|')
                myrouter_parts = str(self.virtual_router).split('|')
                if (router_parts[0] != myrouter_parts[0]) or (router_parts[1] != myrouter_parts[1]):
                    raise ValidationError(" outgoing interface has to be local to virtual router: %s|%s" %
                                  (myrouter_parts[0],myrouter_parts[1]))
        #verify the gateway pool belongs to the local virtual router
        if not 'gateway_pool' in exclude:
            if is_set(self.gateway_pool):
                router_parts = str(self.gateway_pool).split('|')
                myrouter_parts = str(self.virtual_router).split('|')
                if (router_parts[0] != myrouter_parts[0]) or (router_parts[1] != myrouter_parts[1]):
                    raise ValidationError(" gateway pool has to be local to virtual router: %s|%s" %
                                  (myrouter_parts[0],myrouter_parts[1]))

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('virtual_router', 'src_host', 'src_tenant','src_vns','src_ip','src_ip_mask', 'dst_host', 'dst_tenant','dst_vns','dst_ip','dst_ip_mask')

    class Rest:
        NAME = 'virtualrouter-routingrule'
        FIELD_INFO = (
                      {'name': 'virtual_router',           'rest_name': 'virtual-router'},
                      {'name': 'src_tenant',               'rest_name': 'src-tenant'},
                      {'name': 'dst_tenant',               'rest_name': 'dst-tenant'},
                      {'name': 'src_vns',                  'rest_name': 'src-vns'},
                      {'name': 'src_ip',                   'rest_name': 'src-ip'},
                      {'name': 'src_ip_mask',              'rest_name': 'src-ip-mask'},
                      {'name': 'dst_ip',                   'rest_name': 'dst-ip'},
                      {'name': 'dst_ip_mask',              'rest_name': 'dst-ip-mask'},
                      {'name': 'nh_ip',                    'rest_name': 'nh-ip'},
                      {'name': 'outgoing_intf',            'rest_name': 'outgoing-intf'},
                      {'name': 'dst_host',                 'rest_name': 'dst-host'},
                      {'name': 'src_host',                 'rest_name': 'src-host'},   
                      {'name': 'dst_vns',                  'rest_name': 'dst-vns'},
                      {'name': 'gateway_pool',             'rest_name': 'gateway-pool'},
            )
#
# ------------------------------------------------------------
"""
class Tag(models.Model):
    namespace_length = 64
    name_length = 64
    value_length = 64

    #
    # fields ----------------------------------------

    namespace = models.CharField(
        verbose_name='Tag Namespace',
        help_text="Namespace of the tag",
        max_length=namespace_length)

    name = models.CharField(
        verbose_name='Tag Name',
        help_text="Name of the tag",
        max_length=name_length)

    value = models.CharField(
        verbose_name='Tag Value',
        help_text="Value of the tag",
        max_length=value_length)

    persist = models.BooleanField(
        verbose_name='persist',
        help_text='For any cli configured tag, include in running-config',
        default=True)

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('namespace', 'name', 'value')

    class Rest:
        NAME = 'tag'

#
# ------------------------------------------------------------

class TagMapping(models.Model):
    host_id_length = 17
    switch_id_length = 23
    if_name_len = 32
    vlan_str_len = 4
    #
    # fields ----------------------------------------

    tag = models.ForeignKey(
            Tag)

    mac = models.CharField(
        verbose_name="MAC Address",
        max_length=host_id_length, 
        validators = [validate_mac_address],
        default="",
        blank=True)

    vlan = models.CharField(
        verbose_name='VLAN',
        max_length=vlan_str_len, 
        help_text='VLAN Number, in the range of 1-4095. 4095 means untagged',
        validators=[VlanStringValidator()],
        default="",
        blank=True)
    
    dpid = models.CharField(
        verbose_name='Switch DPID',
        help_text='Switch DPID - 64-bit hex separated by :',
        max_length=switch_id_length, 
        validators=[ validate_dpid ],
        default="",
        blank=True)

    ifname = models.CharField(
        verbose_name='If Name regular expression',
        max_length=if_name_len,
        default="",
        validators=[ SafeForPrimaryKeyValidator() ],
        blank=True)

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('tag', 'mac', 'vlan', 'dpid', 'ifname')

    def validate_unique(self, exclude = None):
        if self.mac != '':
            self.mac = self.mac.lower()

        if self.dpid != '':
            self.dpid = self.dpid.lower()

        # don't allow all default values for the association
        if self.mac == '' and self.vlan == '' and \
            self.dpid == '' and self.ifname == '':
           raise ValidationError("Match without any matching fields")

    class Rest:
        NAME = 'tag-mapping'
"""
#
# ------------------------------------------------------------
class TechSupportConf(models.Model):

    #
    # fields ----------------------------------------

    cmd_type = models.CharField(
        verbose_name='Type of command',
        help_text='Enter cli or shell',
        max_length=32)

    cmd = models.CharField(
        max_length=256,
        verbose_name='Command name',
        help_text = 'Command excuted by show tech-support')


    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    class CassandraSettings:
        COMPOUND_KEY_FIELDS = ('cmd_type', 'cmd')

    class Rest:
        NAME = 'tech-support-config'
        FIELD_INFO = (
            {'name': 'cmd_type',  'rest_name': 'cmd-type'},
            )

#
# ------------------------------------------------------------

class TacacsPlusConfig(models.Model):
    #
    # fields ----------------------------------------

    id = models.CharField(
        primary_key=True,
        verbose_name='tacacs singleton',
        default='tacacs',
        max_length=16)

    tacacs_plus_authn = models.BooleanField(
        verbose_name='TACACS+ Authentication Enabled',
        help_text='Enable TACACS+ authentication by setting to true',
        default=False
        )

    tacacs_plus_authz = models.BooleanField(
        verbose_name='TACACS+ Authorization Enabled',
        help_text='Enable TACACS+ authorization by setting to true',
        default=False
        )

    tacacs_plus_acct = models.BooleanField(
        verbose_name='TACACS+ Accounting Enabled',
        help_text='Enable TACACS+ accounting by setting to true',
        default=False
        )

    local_authn = models.BooleanField(
        verbose_name='Local Authentication Enabled',
        help_text='Enable local authentication by setting to true',
        default=True
        )

    local_authz = models.BooleanField(
        verbose_name='Local Authorization Enabled',
        help_text='Enable local authorization by setting to true',
        default=True
        )

    timeout = models.IntegerField(
        verbose_name="TACACS+ Server Timeout",
        help_text='Set TACACS+ server timeout in seconds',
        default=0,
        validators=[ RangeValidator(0, 2**16-1) ])
    
    key = models.CharField(
        verbose_name='TACACS+ Pre-shared Key',
        help_text='pre-shared key to connect to TACACS+ server(s)',
        max_length=256,
        blank=True,
        default="")    

    #
    # end fields ----------------------------------------

    class Rest:
        NAME = 'tacacs-plus-config'
        FIELD_INFO = (
            {'name': 'tacacs_plus_authn', 'rest_name': 'tacacs-plus-authn'},
            {'name': 'tacacs_plus_authz', 'rest_name': 'tacacs-plus-authz'},
            {'name': 'tacacs_plus_acct',  'rest_name': 'tacacs-plus-acct'},
            {'name': 'local_authn',       'rest_name': 'local-authn'},
            {'name': 'local_authz',       'rest_name': 'local-authz'},
            )

#
# ------------------------------------------------------------

class TacacsPlusHost(models.Model):
    #
    # fields ----------------------------------------

    ip = models.CharField(
        primary_key=True,
        verbose_name='IP Address',
        help_text='IP Address for TACACS+ server',
        validators=[ IpValidator() ],
        max_length=15)
    
    timestamp = models.PositiveIntegerField(
        verbose_name='timestamp',
        help_text='Timestamp to order the tacacs servers',
        default = get_timestamp,
        )
    
    key = models.CharField(
        verbose_name='TACACS+ Per-host Pre-shared Key',
        help_text='pre-shared key to connect to this TACACS+ server',
        max_length=256,
        blank=True,
        default="")    

    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return "%s" % self.ip

    def validate_unique(self, exclude = None):
        try:
            exists = TacacsPlusHost.objects.get(ip = self.ip)

            if exists.timestamp != self.timestamp:
                self.timestamp = exists.timestamp
        except:
            pass

    class Rest:
        NAME = 'tacacs-plus-host'
        FIELD_INFO = (
            )

#
#---------------------------------------------------------

class SnmpServerConfig(models.Model):
    #
    # fields ----------------------------------------

    # we just have one row entry in the table to update, so static primary key
    id = models.CharField(
        primary_key=True,
        verbose_name='snmp',
        # default='snmp',
        max_length=16)

    #
    # Enable state of the SNMP server/agent on the controller
    #
    server_enable = models.BooleanField(
        verbose_name='SNMP Server enabled',
        help_text='Enable SNMP server by setting to true',
        default=False
        )
    #
    # Community string for accessing the SNMP server on the controller
    #
    community = models.CharField(
        verbose_name = 'Community String',
        help_text    = "Community String to access SNMP data",
        max_length   = 128,
        null         = True,
        blank        = True,
        )
    #
    # Location string of the SNMP server on the controller
    #
    location = models.CharField(
        verbose_name = 'System Location',
        help_text    = "Location information for the controller appliance",
        max_length   = 128,
        null         = True,
        blank        = True
        )
    #
    # Contact string of the SNMP server on the controller
    #
    contact = models.CharField(
        verbose_name = 'System Contact',
        help_text    = "Contact information for the controller appliance",
        max_length   = 128,
        null         = True,
        blank        = True,
        )
    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id

    def validate_unique(self, exclude = None):
        if self.id != 'snmp':
            raise ValidationError("Only single snmp record exists")

    class Rest:
        NAME = 'snmp-server-config'
        FIELD_INFO = (
            {'name': 'server_enable',       'rest_name': 'server-enable'},
            )

#
# ------------------------------------------------------------

class ImageDropUser(models.Model):
    #
    # fields ----------------------------------------

    # we just have one row entry in the table to update, so static primary key
    id = models.CharField(
        primary_key=True,
        verbose_name='imagedropuser',
        default='imagedropuser',
        max_length=16)

    images_user_ssh_key = models.CharField(
        verbose_name='Image drop user SSH public key',
        help_text='The SSH public RSA key for the images user',
        default='',
        max_length=600
        )
    #
    # end fields ----------------------------------------

    def __unicode__(self):
        return self.id
 
    def validate_unique(self, exclude = None):
        if self.id != 'imagedropuser':
            raise ValidationError("Only single ImageDropUser record exists")

    class Rest:
        NAME = 'image-drop-user'
        FIELD_INFO = (
            {'name': 'images_user_ssh_key', 'rest_name': 'images-user-ssh-key'},
            )

# robv: Commenting out for now. Doesn't work with Cassandra backend
#class ConsoleUser(User):
#
#    class Meta:
#        proxy = True
#
#    class Rest:
#        NAME = 'user'
"""
