import fmtcnv
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
            'action' : 'display-rest',
            'doc'    : 'switch|show',
            'url'    : [
                        'showtunnel',
                       ],
            'format' : 'show_tunnel',
        },
              {
            'choices' : (
                {
                 'field'      : 'showtunnel',
                 'type'       : 'enum',
                 'values'     : ('details',),
                 'action'     : 'display-rest',
                 'url'        : 'showtunnel',
                 'optional'   : True,
                 'format' : 'show_tunnel',
                 'doc'        : 'switch|show-switch-format-+',
                },
                         ),
               }
    )
}