#!/bin/sh

#### Constants #####
REDIS_PATH=${HOME}/apps/redis/src
REDIS_CONF_PATH=${HOME}/conf/redis
BASE_CONF_FILE=${REDIS_CONF_PATH}/redis-base.conf
REDIS_DATA_PATH=${HOME}/redis-data


#### Functions #####
function start
{
    if [ -f $PIDFILE ]; then
        echo "$PIDFILE exists, process is already running or crashed."
        exit 1
    fi

    mkdir -p ${REDIS_DATA_PATH}/$PORT

    echo "Starting Redis..."
    ${REDIS_PATH}/redis-server ${NODE_CONF_FILE}
    echo "DONE."
}

function stop
{
    if [ ! -f $PIDFILE ]; then
        echo "$PIDFILE does not exist, process is not running"
        exit 1
    fi

    PID=$(cat $PIDFILE)
    echo "Stopping Redis..."
    ${REDIS_PATH}/redis-cli -p $PORT shutdown
    while [ -x /proc/${PID} ]
    do
        echo "Waiting for Redis to shutdown ..."
        sleep 1
    done
    echo "DONE."
}


#### Main #####
if [ $# -le 1 ]; then
    echo "Usage: $0 start|stop|restart port"
    exit 1
fi

COMMAND=$1
PORT=$2
NODE_CONF_FILE=${REDIS_CONF_PATH}/redis-node-${PORT}.conf
PIDFILE=${REDIS_DATA_PATH}/${PORT}/redis.pid

if [ ! -f $BASE_CONF_FILE ]; then
    echo "Not exists redis-base.conf. (${BASE_CONF_FILE})"
    exit 1
fi
if [ ! -f $NODE_CONF_FILE ]; then
    echo "Not exists redis-node.conf. (${NODE_CONF_FILE})"
    exit 1
fi

case $COMMAND in
    "start")
        start
    ;;
    "stop")
        stop
    ;;
    "restart")
        stop
        sleep 1
        start
    ;;
    *)
        echo "Usage: $0 start|stop|restart port"
        exit 1
    ;;
esac

exit 0
