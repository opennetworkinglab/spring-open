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
"""
class SyncdConfig(models.Model):
    
    # id is set to the local cluster id.
    id = models.CharField(
        primary_key=True,
        max_length=128)
    
    # Enable syncing to the cloud
    enabled = models.BooleanField(
        verbose_name='Enabled',
        help_text='Is syncing to the cloud enabled',
        default=False)
    
    # Log level for the syncd daemon process
    log_level = models.CharField(
        max_length=32,
        verbose_name='Log Level',
        help_text='Log level for the syncd daemon process',
        default='warning')

    sync_period = models.PositiveIntegerField(
        verbose_name='Sync Period',
        help_text='How often (in seconds) local data is synced to the cloud',
        default=300)
    
    sync_overlap = models.PositiveIntegerField(
        verbose_name='Sync Overlap',
        help_text='Overlap (in seconds) of the time range of a sync iteration with the previous sync iteration',
        default=30)
    
    class Rest:
        NAME = 'syncd-config'
        FIELD_INFO = (
            {'name': 'log_level', 'rest_name': 'log-level'},
            {'name': 'sync_period', 'rest_name': 'sync-period'},
            {'name': 'sync_overlap', 'rest_name': 'sync-overlap'},
        )

class SyncdTransportConfig(models.Model):
    
    # Composite key of <syncd-config-id>:<transport-name>
    id = models.CharField(
        primary_key=True,
        max_length=256)
    
    config = models.ForeignKey(
        SyncdConfig,
        blank=True,
        null=True)

    name = models.CharField(
        max_length=128,
        blank=True,
        null=True)
    
    type = models.CharField(
        max_length=32,
        blank=True,
        null=True)
    
    args = models.TextField(
        blank=True,
        null=True)

    target_cluster = models.CharField(
        max_length=256,
        blank=True,
        null=True)
    
    class Rest:
        NAME = 'syncd-transport-config'
        FIELD_INFO = (
            {'name': 'target_cluster', 'rest_name': 'target-cluster'},
        )

class SyncdProgressInfo(models.Model):
    
    # id value is per-controller-node and per-syncd-transport, so the id is
    # <controller-node id>:<transport-name>
    id = models.CharField(
        primary_key=True,
        max_length=256)

    data_start_time = models.PositiveIntegerField(
        verbose_name='Data Start Time',
        help_text='Timestamp of earliest data available for syncing',
        blank=True,
        null=True)
    
    last_sync_time = models.PositiveIntegerField(
        verbose_name='Last Sync Time',
        help_text='Last time that data was successfully synced to the cloud from the local controller node',
        blank=True,
        null=True)

    class Rest:
        NAME = 'syncd-progress-info'
        FIELD_INFO = (
            {'name': 'data_start_time', 'rest_name': 'data-start-time'},
            {'name': 'last_sync_time', 'rest_name': 'last-sync-time'},
        )
"""