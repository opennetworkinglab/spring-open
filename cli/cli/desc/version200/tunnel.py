import command
import json
import fmtcnv


TUNNEL_SUBMODE_COMMAND_DESCRIPTION = {
    'name'          : 'tunnel',
    'short-help'    : 'Enter tunnel submode, configure tunnel details',
    'mode'          : 'config',
    'parent-field'  : None,
    'command-type'  : 'config-submode',
    'obj-type'      : 'tunnel-config',
    'submode-name'  : 'config-tunnel',
    'doc'           : 'tunnel|tunnel',
    'doc-example'   : 'tunnel|tunnel-example',
    'args' : (
        {
            'field'        : 'tunnel-id',
            'type'         : 'identifier',
            #'completion'   : 'complete-object-field',
            'syntax-help'  : 'Enter a tunnel name',
            'doc'          : 'tunnel|tunnel',
            'doc-include'  : [ 'type-doc' ],
            'completion'   : 'tunnel-id-completion',
            'action'       : (
                                {
                                    'proc' : 'create-tunnel',
                                },
                                {
                                    'proc' : 'push-mode-stack',
                                },
                              ),
            'no-action': (
                {
                    'proc' : 'remove-tunnel',
                }
            ),
        }
    )
}

TUNNEL_CONFIG_FORMAT = {
    'tunnel-config' : {
        'field-orderings' : {
            'default' : [
                          'tunnel-id',
                        ],
        },
    },
}


def tunnel_node_label_completion(prefix, completions):
    #print "tunnel_node_label_completion:",prefix,completions
    query_url = "http://127.0.0.1:8000/rest/v1/switches"
    result = command.sdnsh.store.rest_simple_request(query_url)
    entries = json.loads(result)
    for entry in entries:
        if entry['stringAttributes']['nodeSid'].startswith(prefix):
            completions[entry['stringAttributes']['nodeSid']+' '] = entry['stringAttributes']['nodeSid']
    return

command.add_completion('tunnel-node-label-completion', tunnel_node_label_completion,
                       {'kwargs': { 'prefix'       : '$text',
                                    'completions'  : '$completions',
                                    }})

def tunnel_adjacency_label_completion(prefix, data, completions):
    #print "tunnel_adjacency_label_completion:",prefix,data,completions
    query_url1 = "http://127.0.0.1:8000/rest/v1/switches"
    result1 = command.sdnsh.store.rest_simple_request(query_url1)
    entries1 = json.loads(result1)
    node_dpid = None
    for entry in entries1:
        if (int (entry['stringAttributes']['nodeSid']) == int(data['node-label'])):
            node_dpid = entry['dpid']
    if (node_dpid != None):
        query_url2 = "http://127.0.0.1:8000/rest/v1/router/"+node_dpid+"/adjacency"
        result2 = command.sdnsh.store.rest_simple_request(query_url2)
        entries2 = json.loads(result2)
        for entry in entries2:
            if str(entry.get("adjacencySid")).startswith(prefix):
                completions[str(entry.get("adjacencySid"))+' '] = entry.get("adjacencySid")
    return

command.add_completion('tunnel-adjacency-label-completion', tunnel_adjacency_label_completion,
                       {'kwargs': { 'prefix'       : '$text',
                                    'data'         : '$data',
                                    'completions'  : '$completions',
                                    }})

TUNNEL_ADJACENCY_INFO = (
    {
        'token'               : 'adjacency',
        'short-help'          : 'Set adjacency label on this node',
        'doc'                 : 'tunnel|adjacency',
        'doc-example'         : 'tunnel|adjacency',
    },
    {
         'field'      : 'adjacency-label',
         'type'       : 'label',
         'completion' : 'tunnel-adjacency-label-completion',
         'help-name'  : 'Adjacency label',
         'data'       : {
                          'node_label'      : '$node-label',
                        },
         'action'       : (
                            {
                                'proc' : 'create-tunnel',
                            },
                          ),
    }
)

# obj_type flow-entry field hard-timeout
TUNNEL_NODE_ENTRY_COMMAND_DESCRIPTION = {
    'name'                : 'node',
    'mode'                : 'config-tunnel',
    'short-help'          : 'Set node for this tunnel',
    'doc'                 : 'tunnel|node',
    'doc-example'         : 'tunnel|node',
    'parent-field'        : 'tunnel',
    'command-type'        : 'config',
    'args'                : (
         {
             'field'      : 'node-label',
             'completion'   : 'tunnel-node-label-completion',
             'type'         : 'label',
             'other'        : 'switches|label',
#             'data-handler' : 'alias-to-value',
             'help-name'    : 'Segment label',
             'action'       : (
                                {
                                    'proc' : 'create-tunnel',
                                },
                              ),
         },
         {
            'optional' : True,
            'optional-for-no' : True,
            'args' : TUNNEL_ADJACENCY_INFO,
         },
    )
}

SWITCH_TUNNEL_COMMAND_DESCRIPTION = {
    'name'                : 'show',
    'mode'                : 'login',
    'command-type'        : 'display-table',
    'all-help'            : 'Show switch information',
    'short-help'          : 'Show switch summary',
    #'obj-type'            : 'switches',
    'doc'                 : 'switch|show',
    'doc-example'         : 'switch|show-example',
    'args' : (
        {
            'token'  : 'tunnel',
            'field'  : 'showtunnel',
            'sort'   : ['tunnelId',],
            'action' : 'display-rest',
            'doc'    : 'switch|show',
            'url'    : [
                        'showtunnel',
                       ],
            'format' : 'show_tunnel',
        },
              { 
            'optional'   : True,
            'choices' : (
                {
                 'field'      : 'showtunnel',
                 'type'       : 'enum',
                 'values'     : ('details',),
                 'optional'   : True,
                 'format' : 'show_tunnel',
                 'data'         : { 'detail' : 'details' },
                },
                         ),
               }
    )
}


def tunnel_id_completion(prefix, completions):
    query_url = "http://127.0.0.1:8000/rest/v1/showtunnel"
    result = command.sdnsh.store.rest_simple_request(query_url)
    entries = json.loads(result)
    for entry in entries:
        if entry['tunnelId'].startswith(prefix):
            completions[entry['tunnelId']+' '] = entry['tunnelId']
    return

command.add_completion('tunnel-id-completion', tunnel_id_completion,
                       {'kwargs': { 'prefix'       : '$text',
                                    'completions'  : '$completions',
                                    }})