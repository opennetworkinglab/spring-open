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

#from django.contrib.auth.tests.auth_backends import BackendTest, RowlevelBackendTest, AnonymousUserBackendTest, NoAnonymousUserBackendTest

from django.contrib.auth.tests.basic import BasicTestCase
from django.contrib.auth.tests.decorators import LoginRequiredTestCase
#from django.contrib.auth.tests.forms import UserCreationFormTest, AuthenticationFormTest, SetPasswordFormTest, PasswordChangeFormTest, UserChangeFormTest, PasswordResetFormTest
from django.contrib.auth.tests.remote_user \
        import RemoteUserTest, RemoteUserNoCreateTest, RemoteUserCustomTest
from django.contrib.auth.tests.models import ProfileTestCase
from django.contrib.auth.tests.tokens import TokenGeneratorTest
#from django.contrib.auth.tests.views \
#    import PasswordResetTest, ChangePasswordTest, LoginTest, LogoutTest


from sdncon.clusterAdmin.tests.auth_backends import BackendTest, RowlevelBackendTest, AnonymousUserBackendTest, NoAnonymousUserBackendTest
from sdncon.clusterAdmin.tests.forms import UserCreationFormTest, AuthenticationFormTest, SetPasswordFormTest, PasswordChangeFormTest, UserChangeFormTest, PasswordResetFormTest
from sdncon.clusterAdmin.tests.views \
        import PasswordResetTest, ChangePasswordTest, LoginTest, LogoutTest

# The password for the fixture data users is 'password'
