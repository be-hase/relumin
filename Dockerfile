FROM centos:6

MAINTAINER Ryosuke Hasebe <hsb.1014@gmail.com>

ENV HOME /root
ENV REDIS_VER 3.0.5

# Define working directory.
WORKDIR $HOME

COPY docker/conf $HOME/conf
COPY docker/install $HOME/install
COPY docker/scripts $HOME/scripts

# yum
RUN yum -y groupinstall "Development Tools"
RUN yum -y install python-setuptools wget tar

# supervisor
RUN easy_install supervisor

# install redis
RUN sh $HOME/install/redis/install.sh

# Define default command.
CMD supervisord -c $HOME/conf/supervisord/supervisord.conf

# Expose ports.
EXPOSE 9000 10000 10001 10002 10003 10004 10005 20000 20001 20002 20003 20004 20005 10010 10011 10012 10013 10014 10015
