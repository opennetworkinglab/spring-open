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
# Create your models here.
class StatdropdConfig(models.Model):
    
    # id is set to the local cluster id.
    id = models.CharField(
        primary_key=True,
        max_length=128)

    # Enable dropping local data
    enabled = models.BooleanField(
        verbose_name='Enabled',
        help_text='Is dropping local data enabled.',
        default=True)

    # Log level for the statdropd daemon process
    log_level = models.CharField(
        max_length=32,
        verbose_name='Log Level',
        help_text='Log level for the statdropd daemon process',
        default='warning')
    
    period = models.PositiveIntegerField(
        verbose_name='Period',
        help_text='How often (in seconds) to drop local stats/log data',
        default=600)
    
    #retain_synced_data_duration = models.PositiveIntegerField(
    #    verbose_name='Retain Synced Data',
    #    help_text="How long should data that's been synced to the cloud be retained locally",
    #    default=604800)
    
    retain_data_duration = models.PositiveIntegerField(
        verbose_name='Retain Data Duration',
        help_text="How long should data be retained locally",
        default=604800)
    
    class Rest:
        NAME = 'statdropd-config'
        FIELD_INFO = (
            {'name': 'log_level', 'rest_name': 'log-level'},
            #{'name': 'retain_synced_data_duration', 'rest_name': 'retain-synced-data-duration'},
            {'name': 'retain_data_duration', 'rest_name': 'retain-data-duration'},
        )

class StatdropdProgressInfo(models.Model):
    
    # id value is <controller-node-id>
    id = models.CharField(
        primary_key=True,
        max_length=256)

    last_drop_point = models.PositiveIntegerField(
        verbose_name='Last Drop Point',
        help_text='Last point when stats data was successfully dropped from the local controller node')
    
    class Rest:
        NAME = 'statdropd-progress-info'
        FIELD_INFO = (
            {'name': 'last_drop_point', 'rest_name': 'last-drop-point'},
        )
"""