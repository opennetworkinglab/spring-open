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

import datetime
from django.db import models
from django.contrib.auth.models import User

# Create your models here.
class CustomerManager(models.Manager):

    def create_customer(self, name, email):
        """
        Creates and saves a customer with the given name and email
        """
        now = datetime.datetime.now()

        # Normalize the address by lowercasing the domain part of the email address
        try:
            email_name, domain_part = email.strip().split('@', 1)
        except ValueError:
            pass
        else:
            email = '@'.join([email_name, domain_part.lower()])

        customer = self.model(customername=name, is_active=True, date_joined=now)
        customer.save(using=self._db)
        return customer

class ClusterManager(models.Manager):

    def create_cluster(self, name, customer):
        """
        Creates and saves a cluster with the given cluster name and the customer
        """
        print "clusterManager create_cluster"
        now = datetime.datetime.now()

        id = ":".join([customer.customername, name])
        cluster = self.model(id=id, clustername=name, is_active=True, date_joined=now)
        cluster.save(using=self._db)
        return cluster

class CustomerUserManager(models.Manager):

    def create_customerUser(self, user, customer):
        """
        Creates and saves a customer user membership
        """
        id = ":".join([customer.customername, user.username])
        cu = self.model(id=id, user=user, customer=customer)
        cu.save(using=self._db)
        return cu

class Customer(models.Model):
    """
    Customer defines a customer in the cloud server.
    """
    customername = models.CharField('customer name', 
                    primary_key=True,
                    max_length=30, 
                    help_text="30 characters or fewer. Letters, numbers and @/./+/-/_ characters")
    email = models.EmailField('e-mail address', blank=True)
    is_active = models.BooleanField('active', 
                    default=True, 
                    help_text="Designates whether this Customer should be treated as active. Unselect this instead of deleting accounts.")
    date_joined = models.DateTimeField('date joined', default=datetime.datetime.now)
    objects = CustomerManager()

    def __unicode__(self):
        return self.customername

class Cluster(models.Model):
    """
    Cluster defines a cluster of nodes in the cloud server.
    """
    id = models.CharField(
                primary_key=True,
                verbose_name='Cluster ID',
                max_length=75,
                help_text='Unique identifier for the cluster; format is customername:clustername')
    clustername = models.CharField('cluster name', 
                    max_length=30, 
                    help_text="Required. 30 characters or fewer. Letters, numbers and @/./+/-/_ characters")
    is_active = models.BooleanField('active', 
                    default=True, 
                    help_text="Designates whether this cluster should be treated as active. Unselect this instead of deleting the cluster.")
    date_joined = models.DateTimeField('date joined', 
                    default=datetime.datetime.now)

    customer = models.ForeignKey(Customer, on_delete=models.CASCADE)

    objects = ClusterManager()

    def __unicode__(self):
        return self.id


class CustomerUser(models.Model):
    """
    This is a temporary model that captures the list of users in a given customer
    """
    id = models.CharField(
                primary_key=True,
                verbose_name='Customer User',
                max_length=75,
                help_text='Unique relationship that shows the customer which the user belongs; \
                    format is customername:username')
    user = models.ForeignKey(User, on_delete=models.CASCADE)
    customer = models.ForeignKey(Customer, on_delete=models.CASCADE)

    objects = CustomerUserManager()

    def __unicode__(self):
        return self.id


class AuthToken(models.Model):
    """
    Store the authentication token as an ascii string
    Associate various credential options: user, cluster, customer.  At least
    one of these must be populated to be a valid entry.
    """
    id = models.CharField(
        primary_key=True,
        max_length = 64)

    cluster = models.ForeignKey(
        Cluster,
        blank=True,
        null=True)

    user = models.ForeignKey(
        User,
        blank=True,
        null=True)

    customer = models.ForeignKey(
        Customer,
        blank=True,
        null=True)

    expiration_date = models.DateTimeField(
        verbose_name='Expiration Date',
        help_text='Date when the authentication token expires',
        blank=True,
        null=True)

    annotation = models.CharField(
        verbose_name='Annotation',
        help_text='Track creation information such as user',
        max_length=512,
        blank=True,
        null=True)

    def __unicode__(self):
        return str(self.id)

