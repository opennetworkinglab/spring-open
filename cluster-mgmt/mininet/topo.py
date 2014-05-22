"""
This is sample config file for start_topo.py.
Please rename to topo.`hostname`.py before use.
"""
def createTopo():
  return {
    "controllers": {
        "onosdev1" : "192.168.56.11:6633",
        "onosdev2" : "192.168.56.12:6633"
    },
    "switches": {
        "sw01": { "dpid": "0000000000000101", "controllers": [ "onosdev1" ] },
        "sw02": { "dpid": "0000000000000102", "controllers": [ "onosdev1" ] },
        "sw03": { "dpid": "0000000000000103", "controllers": [ "onosdev1" ] },
        "sw04": { "dpid": "0000000000000104", "controllers": [ "onosdev2" ] },
        "sw05": { "dpid": "0000000000000105", "controllers": [ "onosdev2", "onosdev1" ] }
    },
    "hosts": {
        "h01": {},
        "h02": {},
        "h03": {},
        "h04": {},
        "h05": {}
    },
    "links": [
        {
            "node1": "sw01",
            "node2": "sw02"
        },{
            "node1": "sw02",
            "node2": "sw03"
        },{
            "node1": "sw03",
            "node2": "sw04"
        },{
            "node1": "sw04",
            "node2": "sw05"
        },{
            "node1": "sw05",
            "node2": "sw01"
        },{
            "node1": "h01",
            "node2": "sw01"
        },{
            "node1": "h02",
            "node2": "sw02"
        },{
            "node1": "h03",
            "node2": "sw03"
        },{
            "node1": "h04",
            "node2": "sw04"
        },{
            "node1": "h05",
            "node2": "sw05"
        }
    ]
}