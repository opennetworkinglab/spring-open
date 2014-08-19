#!/usr/bin/env python
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

#
# Copyright (c) 2011,2012 Big Switch Networks, Inc.
# All rights reserved.
#

from sys import stderr
from re import compile, match, search
from optparse import OptionParser
from collections import defaultdict
from subprocess import Popen, PIPE, STDOUT

import socket, fcntl, struct
def get_ipaddr(ifname):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    ipaddr = fcntl.ioctl(s.fileno(), 0x8915, struct.pack('256s', ifname[:15]))  # SIOCGIFADDR
    return socket.inet_ntoa(ipaddr[20:24])

import urllib, json
def get_aliases(controller='localhost', controller_port=6633, ifname='eth0'):
    url = "http://%s/rest/v1/model/switch/" % controller
    aliases = {get_ipaddr(ifname)+':%d'%controller_port: 'controller'}
    for switch in json.loads(urllib.urlopen(url).read()):
        socketaddr = switch.get('socket-address')
        if socketaddr:
            socketaddr = socketaddr[1:]
        alias = switch.get('alias')
        if not alias:
            alias = switch.get('dpid')
        aliases[socketaddr] = alias
    return aliases

DEBUG=False

class OpenFlowTracer(object):
    # Formats
    SUMMARY = "summary"
    DETAIL = "detail"
    ONELINE = "oneline"

    def __init__(self, msg_filters=None, tcpdump_filter='', fmt=SUMMARY, alias=False, printer=None):
        self.msg_filters = msg_filters
        self.tcpdump_filter = tcpdump_filter
        self.detail = (fmt != self.SUMMARY)
        self.oneline = (fmt == self.ONELINE)
        self.printer = self.prn
        if printer:
            self.printer = printer

        self.pkt = defaultdict(str)
        self.reading_detail = False
        self.alias = alias
        self.aliases = None
        if self.alias:
            self.aliases = get_aliases()

    def run(self):
        self.tcpdump_trace(self.tcpdump_open())
        return

    # filter packets
    def filter(self):
        msg_type =  self.pkt['of-msg']
        if not msg_type:
            return False
        if self.msg_filters:
            return any(map(lambda pattern: search(pattern, msg_type), self.msg_filters))
        return True

    # format packet output
    def fmt(self):
        pkt = self.pkt
        sep = '\n    '
        if self.oneline: sep = ', '

        src = '%s:%s' % (pkt['src-ip'], pkt['src-port'])
        dst = '%s:%s' % (pkt['dst-ip'], pkt['dst-port'])
        if self.alias:
            if not self.aliases.get(src) or not self.aliases.get(dst):
                self.aliases = get_aliases()
            src = self.aliases.get(src, src)
            dst = self.aliases.get(dst, dst)

        ret = '%-18s %-18s [ %s -> %s ]' % (
            pkt['timestamp'], pkt['of-msg'], src, dst)
        if self.detail and pkt['of-data']:
            for data in pkt['of-data'].split(','):
                data = data.strip()
                if data:
                    ret += (sep + data)
        return ret

    def prn(self, x):
        print x

    def tcpdump_open(self):
        tcpdump_cmd = 'sudo /usr/local/sbin/tcpdump -i any \'%s\' %s'
        tcpdump_args = '-l -nn -vv'
        self.process = Popen(tcpdump_cmd % (self.tcpdump_filter, tcpdump_args),
                        shell=True, bufsize=1, stdout=PIPE, stderr=STDOUT)
        return self.process.stdout

    def tcpdump_trace(self, fd):
        # tcpdump decoder patterns
        empty_line = compile('^$')
        tcpdump_line = compile('^tcpdump.*$')
        ip_line = compile('^(\d{2}\:\d{2}:\d{2}\.\d+) IP .*$')
        tcp_line = compile('^\s*(\d+\.\d+\.\d+\.\d+)\.(\d+) \> (\d+\.\d+\.\d+\.\d+)\.(\d+)\:.*length \d+(.*)$')
        of_hdr = compile('^([^ ]*) \(xid\=([^\)]*)\)\:(.*)$')
        of_line = compile('^(.*in_port\=.*)$')
        if_cont_line = compile('^\s+current\:')
        inline_err = compile('^\s*\(\*\*\*.*$')
        pkt_count = compile('^\d+ packets ')

        # helper fn
        def of_data(pkt, data):
            if pkt['of-msg'] == 'packet_out':
                data = data.replace(' ', ',')
            pkt['of-data'] += (',' + data)

        # decode packet data
        line = fd.readline()
        while line:
            try:
                line = line.strip()
                pkt = self.pkt
                if empty_line.match(line):
                    pass
                elif pkt_count.match(line):
                    print>>stderr, line
                elif tcpdump_line.match(line):
                    pass
                elif ip_line.match(line):
                    if self.filter(): self.printer(self.fmt())
                    self.pkt = defaultdict(str)
                    self.reading_detail = False
                    pkt = self.pkt
                    pkt['timestamp'] = ip_line.match(line).groups()[0]
                elif tcp_line.match(line):
                    m =  tcp_line.match(line).groups()
                    pkt['src-ip'] = m[0]
                    pkt['src-port'] = m[1]
                    pkt['dst-ip'] = m[2]
                    pkt['dst-port'] = m[3]
                    if of_hdr.match(m[4]):
                        self.reading_detail = True
                        m = of_hdr.match(m[4]).groups()
                        pkt['of-msg'] = m[0].strip()
                        pkt['of-xid'] = m[1]
                        data = m[2]
                        if not inline_err.match(m[2]):
                            of_data(pkt, data)
                        else:
                            if DEBUG:
                                print>>stderr, 'Inline Error', data
                elif of_line.match(line):
                    of_data(pkt, of_line.match(line).groups()[0])
                elif if_cont_line.match(line):
                    line = line.replace(',', '; ')
                    of_data(pkt, '; '+line)
                elif self.reading_detail:
                    line = line.replace(',', '; ')
                    of_data(pkt, line)
                else:
                    if DEBUG:
                        print>>stderr, 'Unexpected: Line', line
                line = fd.readline()
            except KeyboardInterrupt:
                print
                if self.process:
                    self.process.terminate()
                    self.process = None
                else:
                    break

def main():
    usage = "usage: %prog [--detail] [--oneline] [--filter <tcpdump-filter>] [<msg-type> ...]"
    optparser = OptionParser(usage=usage)
    optparser.add_option("-f", "--filter", dest="tcpdump_filter",
                         action="store", type="string", default='((tcp) and (port 6633))',
                         help="tcpdump filter rule", metavar="FILTER")
    optparser.add_option("-d", "--detail", dest="detail",
                         action="store_true", default=False,
                         help="print message detail")
    optparser.add_option("-o", "--oneline", dest="oneline",
                         action="store_true", default=False,
                         help="print message detail on one line")
    optparser.add_option("-a", "--alias", dest="alias",
                         action="store_true", default=False,
                         help="query controller to resolve address to switch-dpid/alias")
    (options, msg_filters) = optparser.parse_args()

    try:
        print 'Starting openflow trace, use ^C to quit'
        fmt = OpenFlowTracer.SUMMARY
        if options.detail:  fmt = OpenFlowTracer.DETAIL
        if options.oneline:  fmt = OpenFlowTracer.ONELINE
        tracer = OpenFlowTracer(msg_filters, options.tcpdump_filter, fmt=fmt, alias=options.alias)
        tracer.run()
    except KeyboardInterrupt:
        pass

if __name__ == '__main__':
    main()
