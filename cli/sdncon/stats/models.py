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
class StatdConfig(models.Model):
    stat_type = models.CharField(
        primary_key=True,
        verbose_name='Stat Type',
        max_length=64)
    
    sampling_period = models.PositiveIntegerField(
        verbose_name='Sampling Period',
        default=15)
    
    reporting_period = models.PositiveIntegerField(
        verbose_name='Reporting Period',
        default=60)
    
    class Rest:
        NAME = 'statd-config'
        FIELD_INFO = (
            {'name': 'stat_type', 'rest_name': 'stat-type'},
            {'name': 'sampling_period', 'rest_name': 'sampling-period'},
            {'name': 'reporting_period', 'rest_name': 'reporting-period'},
            )
"""