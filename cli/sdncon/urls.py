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

import django.views.generic.simple, os, coreui.models
from django.conf.urls.defaults import *
from django.contrib import admin
from apploader import AppLoader

admin.autodiscover()

urlpatterns = patterns('',
    # Example:
    # (r'^sdncon/', include('sdncon.foo.urls')),

    # Uncomment the admin/doc line below and add 'django.contrib.admindocs' 
    # to INSTALLED_APPS to enable admin documentation:
    #(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    ###################################
    # Views for the web UI
    ###################################
    (r'^$', 'sdncon.ui.views.index'),
    (r'ui/show_switch', 'sdncon.ui.views.show_switch'),
    (r'ui/show_host', 'sdncon.ui.views.show_host'),
    (r'ui/show_link', 'sdncon.ui.views.show_link'),
    (r'ui/show_tunnel', 'sdncon.ui.views.show_tunnel'),
    (r'ui/build_topology', 'sdncon.ui.views.build_topology'),
    # Uncomment the next line to enable the admin:
    (r'^admin/', include(admin.site.urls)),
    (r'accounts/login/$', 'django.contrib.auth.views.login'),
    (r'logout/$', 'django.contrib.auth.views.logout'),
    (r'password_change/$', 'django.contrib.auth.views.password_change'),
    (r'password_change_done/$', 'django.contrib.auth.views.password_change_done'),
    (r'token/?$', 'sdncon.clusterAdmin.views.token_view'),
    (r'token_generate/?$', 'sdncon.clusterAdmin.views.token_generate'),

    ###################################
    # Views that implement the REST API
    ###################################
    (r'^rest/v1/data/?$', 'sdncon.rest.views.do_user_data_list'),
    (r'^rest/v1/data/(?P<name>[A-Za-z0-9_:./=;\-]+)/$', 'sdncon.rest.views.do_user_data'),

    # APIs to get tenant info and build type
    (r'^rest/v1/customer/?$', 'sdncon.clusterAdmin.views.session_clusterAdmin'),
    (r'^rest/v1/personality/?$', 'sdncon.clusterAdmin.views.get_node_personality'),

    # REST API to access statistics counters from sdnplatform
    (r'^rest/v1/realtimestats/counter/categories/(?P<dpid>[A-Za-z0-9_:.\-]+)/(?P<stattype>[A-Za-z0-9_:.\-]+)/(?P<layer>[0-9])/?$', 'sdncon.rest.views.do_sdnplatform_counter_categories'),
    (r'^rest/v1/realtimestats/counter/(?P<stattype>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.rest.views.do_sdnplatform_realtimestats'),
    (r'^rest/v1/realtimestats/counter/(?P<dpid>[A-Za-z0-9_:.\-]+)/(?P<stattype>[A-Za-z]+)/?$', 'sdncon.rest.views.do_sdnplatform_realtimestats'),
    (r'^rest/v1/realtimestats/table/(?P<tabletype>[A-Za-z]+)/flow/(?P<dpid>[A-Za-z0-9_:./\-]+)/?$', 'sdncon.rest.views.do_tablerealtimestats'),
    (r'^rest/v1/realtimestats/group/(?P<groupId>[0-9]+)/(?P<dpid>[A-Za-z0-9_:./\-]+)/?$', 'sdncon.rest.views.do_realtimegroupstats'),
    (r'^rest/v1/realtimestats/(?P<stattype>[A-Za-z]+)/(?P<dpid>[A-Za-z0-9_:./\-]+)/?$', 'sdncon.rest.views.do_realtimestats'),
    (r'^rest/v1/controller/stats/(?P<stattype>[A-Za-z]+)/?$', 'sdncon.rest.views.do_controller_stats'),
    (r'^rest/v1/controller/storage/tables/?$', 'sdncon.rest.views.do_controller_storage_table_list'),
    

    # REST API VNS/device information
    (r'^rest/v1/device', 'sdncon.rest.views.do_device'),
    (r'^rest/v1/switches', 'sdncon.rest.views.do_switches'),
    (r'^rest/v1/routers', 'sdncon.rest.views.do_routers'),
    (r'^rest/v1/router/(?P<dpid>[A-Za-z0-9_:.\-]+)/(?P<stattype>[A-Za-z]+)', 'sdncon.rest.views.do_router_stats'),
    (r'^rest/v1/links', 'sdncon.rest.views.do_links'),
    (r'^rest/v1/mastership', 'sdncon.rest.views.do_mastership'),
    (r'^rest/v1/vns/device-interface', 'sdncon.rest.views.do_vns_device_interface'),
    (r'^rest/v1/vns/interface', 'sdncon.rest.views.do_vns_interface'),
    (r'^rest/v1/vns/realtimestats/flow/(?P<vnsName>[A-Za-z0-9_:.|\-]+)/?$', 'sdncon.rest.views.do_vns_realtimestats_flow', {'category': 'vns'}),
    (r'^rest/v1/vns', 'sdncon.rest.views.do_vns'),

    # REST API to access realtime status from sdnplatform
    (r'^rest/v1/realtimestatus/network/tunnelstatus/all/all/?$', 'sdncon.rest.views.do_topology_tunnel_status'),
    (r'^rest/v1/realtimestatus/network/tunnelstatus/(?P<srcdpid>[A-Za-z0-9_:.\-]+)/(?P<dstdpid>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.rest.views.do_topology_tunnel_status'),
    (r'^rest/v1/realtimestatus/network/tunnelverify/(?P<srcdpid>[A-Za-z0-9_:.\-]+)/(?P<dstdpid>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.rest.views.do_topology_tunnel_verify'),
    (r'^rest/v1/realtimestatus/network/(?P<subcategory>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.rest.views.do_sdnplatform_realtimestatus', {'category': 'network'}),

    # REST API for testing with Explain Packet in realtime
    (r'^rest/v1/realtimetest/network/(?P<subcategory>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.rest.views.do_sdnplatform_realtimetest', {'category': 'network'}),
    
    # REST API for accessing controller system information
    (r'^rest/v1/system/version/?$', 'sdncon.rest.views.do_system_version', {}),
    (r'^rest/v1/system/interfaces/?$', 'sdncon.rest.views.do_system_inet4_interfaces', {}),
    (r'^rest/v1/system/uptime/?$', 'sdncon.rest.views.do_system_uptime', {}),
    (r'^rest/v1/system/clock/utc/?$', 'sdncon.rest.views.do_system_clock', {'local': False}),
    (r'^rest/v1/system/clock/local/?$', 'sdncon.rest.views.do_system_clock', {'local': True}),
    (r'^rest/v1/system/timezones/(?P<list_type>[A-Za-z0-9_\-]+)/?$', 'sdncon.rest.views.do_system_time_zone_strings'),
    (r'^rest/v1/system/controller/?$', 'sdncon.rest.views.do_controller'),
    (r'^rest/v1/system/ha/role/?$', 'sdncon.rest.views.do_local_ha_role'),
    (r'^rest/v1/system/ha/failback/?$', 'sdncon.rest.views.do_ha_failback'),
    (r'^rest/v1/system/ha/provision/?$', 'sdncon.rest.views.do_ha_provision'),
    (r'^rest/v1/system/ha/clustername/?$', 'sdncon.rest.views.do_clustername'),
    (r'^rest/v1/system/ha/decommission/?$', 'sdncon.rest.views.do_decommission'),
    (r'^rest/v1/system/ha/decommission-internal/?$', 'sdncon.rest.views.do_decommission_internal'),
    (r'^rest/v1/system/check-config/?$', 'sdncon.rest.views.do_check_config'),
    (r'^rest/v1/system/reload/?$', 'sdncon.rest.views.do_reload'),
    (r'^rest/v1/system/resetbsc/?$', 'sdncon.rest.views.do_resetbsc'),
    (r'^rest/v1/system/delete-images-passwd/?$', 'sdncon.rest.views.do_delete_images_passwd'),
    (r'^rest/v1/system/upload-data/?$', 'sdncon.rest.views.do_upload_data'),
    (r'^rest/v1/system/log/$', 'sdncon.rest.views.do_system_log_list'),
    (r'^rest/v1/system/log/(?P<log_name>[A-Za-z0-9_\-]+)$', 'sdncon.rest.views.do_system_log'),
    
    # REST API for upgrades
    (r'^rest/v1/system/upgrade/extract-image-manifest/?$', 'sdncon.rest.views.do_extract_upgrade_pkg_manifest'),
    (r'^rest/v1/system/upgrade/image-name/?$', 'sdncon.rest.views.do_get_upgrade_pkg'),
    (r'^rest/v1/system/upgrade/execute-upgrade-step/?$', 'sdncon.rest.views.do_execute_upgrade_step'),
    (r'^rest/v1/system/upgrade/cleanup-old-images/?$', 'sdncon.rest.views.do_cleanup_old_pkgs'),
    (r'^rest/v1/system/upgrade/abort/?$', 'sdncon.rest.views.do_abort_upgrade'),
   
     # REST API for config rollback
    (r'^rest/v1/system/rollback/config/?$', 'sdncon.rest.views.do_config_rollback'),
    (r'^rest/v1/system/rollback/diffconfig/?$', 'sdncon.rest.views.do_diff_config'),

    # Views for the stats REST API
    (r'^rest/v1/stats/metadata/(?P<cluster>[A-Za-z0-9_:.\-]+)(/(?P<stats_type>[A-Za-z0-9_\-]+))?/?$', 'sdncon.stats.views.do_get_stats_metadata'),
    (r'^rest/v1/stats/index/(?P<cluster>[A-Za-z0-9_:.\-]+)/(?P<target_type>[A-Za-z0-9_\-]+)/(?P<target_id>[A-Za-z0-9_\:\.\-]+)(/(?P<stats_type>[A-Za-z0-9_\-]+))?/?$', 'sdncon.stats.views.do_get_stats_type_index'),
    (r'^rest/v1/stats/target/(?P<cluster>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.stats.views.do_get_stats_target_types'),
    (r'^rest/v1/stats/target/(?P<cluster>[A-Za-z0-9_:.\-]+)/(?P<target_type>[A-Za-z0-9_\-]+)/?$', 'sdncon.stats.views.do_get_stats_targets'),
    (r'^rest/v1/stats/data/(?P<cluster>[A-Za-z0-9_:.\-]+)/(?P<target_type>[A-Za-z0-9_\-]+)/(?P<target_id>[A-Za-z0-9_\:\.\-]+)/(?P<stats_type>[A-Za-z0-9_\-]+)/?$', 'sdncon.stats.views.do_get_stats'),
    (r'^rest/v1/stats/data/(?P<cluster>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.stats.views.do_put_stats'),
    (r'^rest/v1/events/data/(?P<cluster>[A-Za-z0-9_:.\-]+)/(?P<node_id>[A-Za-z0-9_\.\-]+)/?$', 'sdncon.stats.views.do_get_events'),
    (r'^rest/v1/events/data/(?P<cluster>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.stats.views.do_put_events'),
    
    # Views for the models
    (r'^rest/v1/model/(?P<model_name>[A-Za-z0-9_\-]+)(/(?P<id>[A-Za-z0-9_:.\-|]+))?/?$', 'sdncon.rest.views.do_instance'),
    # Get the model list
    (r'^rest/v1/model/?$', 'sdncon.rest.views.do_model_list'),

    # Views for the synthesized table
    (r'^rest/v1/synthetic/(?P<model_name>[A-Za-z0-9_\-]+)(/(?P<id>[A-Za-z0-9_:.\-|]+))?/?$', 'sdncon.rest.views.do_synthetic_instance'),


    # REST API for packet tracing
    (r'^rest/v1/packettrace/?$', 'sdncon.rest.views.do_packettrace'),

    # Views for the coreui (base layout and javascript)
    (r'^coreui/static/(?P<path>.*)$', 'django.views.static.serve', {'document_root': os.path.join(os.path.dirname(__file__)+'/coreui','static')}),
    (r'^coreui/img/(?P<path>.*)$', 'django.views.static.serve', {'document_root': os.path.join(os.path.dirname(__file__)+'/coreui','img')}),

    # REST APIs for Performance Monitoring information of SDNPlatform and SDNPlatform
    (r'^rest/v1/performance-monitor/(?P<type>[A-Za-z0-9_.\-]+)/?$', 'sdncon.rest.views.do_sdnplatform_performance_monitor', {'category':'performance-monitor'}),

    # REST APIs for Internal debugging information of SDNPlatform and SDNPlatform
    (r'^rest/v1/internal-debugs/(?P<component>[A-Za-z0-9\-]+)/(?P<query>[A-Za-z0-9=:\-]+)?/?$', 'sdncon.rest.views.do_sdnplatform_internal_debugs', {'category':'internal-debugs'}),

    # REST APIs for Event History from SDNPlatform and SDNPlatform
    (r'^rest/v1/event-history/(?P<evHistName>[A-Za-z0-9\-]+)/(?P<count>[0-9]+)?/?$', 'sdncon.rest.views.do_sdnplatform_event_history', {'category':'event-history'}),

    # REST APIs for displaying the content of Flow Cache in real-time
    # applName = Application Name (e.g. vns)
    # applInstName = Application Instance Name (e.g. name of the vns or "all")
    (r'^rest/v1/flow-cache/(?P<applName>[A-Za-z0-9\-]+)/(?P<applInstName>[A-Za-z0-9:.|\-]+)/(?P<queryType>[A-Za-z0-9\-]+)?/?$', 'sdncon.rest.views.do_flow_cache', {'category':'flow-cache'}),
    #(r'^rest/v1/flow-cache/(?P<applName>[A-Za-z0-9\-]+)/(?P<applInstName>[A-Za-z0-9:.|\-]+)/(?P<queryType>[A-Za-z0-9\-]+)?/?$', 'sdncon.rest.views.do_local', { 'url' : [ 'flow-cache' ], }),

    # REST APIs for Tunnel manager state from SDNPlatform
    (r'^rest/v1/tunnel-manager/(?P<dpid>[A-Za-z0-9_:.\-]+)/?$', 'sdncon.rest.views.do_sdnplatform_tunnel_manager'),

    # REST APIs for controller summary statistics 
    (r'^rest/v1/controller/summary$', 'sdncon.rest.views.do_sdnplatform_controller_summary'),
    
    # REST APIs for SR tunnel 
    (r'^rest/v1/tunnel/?$', 'sdncon.rest.views.do_sdnplatform_tunnel_config'),
    (r'^rest/v1/showtunnel/?$', 'sdncon.rest.views.do_show_tunnel'),
    # REST APIs for SR policy 
    (r'^rest/v1/showpolicy/?$', 'sdncon.rest.views.do_show_policy'),
    (r'^rest/v1/policy/?$', 'sdncon.rest.views.do_sdnplatform_policy_config'),

)

