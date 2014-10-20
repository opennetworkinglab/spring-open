#import fmtcnv
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
            'token'  : 'policy',
            'field'  : 'showpolicy',
            'action' : 'display-rest',
            'doc'    : 'switch|show',
            'url'    : [
                        'showpolicy',
                       ],
            'format' : 'show_policy',
        },
    )
}