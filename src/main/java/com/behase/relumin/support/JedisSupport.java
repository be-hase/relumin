package com.behase.relumin.support;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.util.ValidationUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JedisSupport {
    public Jedis getJedisByHostAndPort(String hostAndPort, int timeout) {
        String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
        return new Jedis(hostAndPortArray[0], Integer.valueOf(hostAndPortArray[1]), timeout);
    }

    public Jedis getJedisByHostAndPort(String hostAndPort) {
        String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
        return new Jedis(hostAndPortArray[0], Integer.valueOf(hostAndPortArray[1]));
    }

    public JedisCluster getJedisClusterByHostAndPort(String hostAndPort) {
        String[] hostAndPortArray = StringUtils.split(hostAndPort, ":");
        return new JedisCluster(
                Sets.newHashSet(new HostAndPort(hostAndPortArray[0], Integer.valueOf(hostAndPortArray[1]))),
                2000,
                new JedisPoolConfig());
    }

    public JedisCluster getJedisClusterByHostAndPorts(String hostAndPorts) {
        Set<HostAndPort> hostAndPortSet = getHostAndPorts(Splitter.on(",").trimResults().omitEmptyStrings().splitToList(hostAndPorts)).stream().map(v -> {
            String[] hostAndPortArray = StringUtils.split(v, ":");
            return new HostAndPort(hostAndPortArray[0], Integer.valueOf(hostAndPortArray[1]));
        }).collect(Collectors.toSet());
        return new JedisCluster(hostAndPortSet, 2000, new JedisPoolConfig());
    }

    public Map<String, String> parseInfoResult(String result) {
        Map<String, String> map = Maps.newLinkedHashMap();
        map.put("_timestamp", String.valueOf(System.currentTimeMillis()));

        String[] line = StringUtils.split(result, "\n");
        for (String each : line) {
            String[] eachArray = StringUtils.split(each, ":");
            if (eachArray.length != 2) {
                continue;
            }
            String key = StringUtils.trim(eachArray[0]);
            String value = StringUtils.trim(eachArray[1]);
            map.put(key, value);
        }

        return map;
    }

    public Map<String, String> parseClusterInfoResult(String result) {
        Map<String, String> map = Maps.newLinkedHashMap();

        String[] line = StringUtils.split(result, "\n");
        for (String each : line) {
            String[] eachArray = StringUtils.split(each, ":");
            if (eachArray.length != 2) {
                continue;
            }
            String key = StringUtils.trim(eachArray[0]);
            String value = StringUtils.trim(eachArray[1]);
            map.put(key, value);
        }

        return map;
    }

    public List<ClusterNode> parseClusterNodesResult(String result, String hostAndPort) {
        List<ClusterNode> clusterNodes = Lists.newArrayList();

        for (String resultLine : StringUtils.split(result, "\n")) {
            ClusterNode clusterNode = new ClusterNode();

            String[] resultLineArray = StringUtils.split(resultLine);
            clusterNode.setNodeId(resultLineArray[0]);

            String eachHostAndPort = resultLineArray[1];
            if (StringUtils.isBlank(hostAndPort)) {
                clusterNode.setHostAndPort(eachHostAndPort);
            } else {
                if (StringUtils.startsWith(eachHostAndPort, ":")) {
                    clusterNode.setHostAndPort(hostAndPort);
                } else {
                    String[] eachHostAndPortArray = StringUtils.split(eachHostAndPort, ":");
                    if ("127.0.0.1".equals(eachHostAndPortArray[0]) || "localhost".equals(eachHostAndPortArray[0])) {
                        clusterNode.setHostAndPort(hostAndPort);
                    } else {
                        clusterNode.setHostAndPort(eachHostAndPort);
                    }
                }
            }

            String eachFlag = resultLineArray[2];
            List<String> eachFlagList = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(eachFlag);
            Set<String> eachFlagSet = Sets.newLinkedHashSet(eachFlagList);
            clusterNode.setFlags(eachFlagSet);

            clusterNode.setMasterNodeId("-".equals(resultLineArray[3]) ? "" : resultLineArray[3]);

            clusterNode.setPingSent(Long.valueOf(resultLineArray[4]));
            clusterNode.setPongReceived(Long.valueOf(resultLineArray[5]));

            clusterNode.setConfigEpoch(Long.valueOf(resultLineArray[6]));

            clusterNode.setConnect("connected".equals(resultLineArray[7]));

            List<String> slots = Lists.newArrayList();
            for (int i = 8; i < resultLineArray.length; i++) {
                if (clusterNode.hasFlag("myself") && StringUtils.startsWith(resultLineArray[i], "[")) {
                    String trimed = StringUtils.substring(resultLineArray[i], 1, -1);
                    if (StringUtils.indexOf(trimed, "->-") != StringUtils.INDEX_NOT_FOUND) {
                        String[] trimedArray = StringUtils.split(trimed, "->-");
                        clusterNode.getMigrating().put(Integer.valueOf(trimedArray[0]), trimedArray[1]);
                    } else if (StringUtils.indexOf(trimed, "-<-") != StringUtils.INDEX_NOT_FOUND) {
                        String[] trimedArray = StringUtils.split(trimed, "-<-");
                        clusterNode.getImporting().put(Integer.valueOf(trimedArray[0]), trimedArray[1]);
                    }
                } else {
                    slots.add(resultLineArray[i]);
                }
            }

            slots.forEach(v -> {
                if (StringUtils.indexOf(v, "-") == StringUtils.INDEX_NOT_FOUND) {
                    clusterNode.getServedSlotsSet().add(Integer.valueOf(v));
                } else {
                    String[] startAndEnd = StringUtils.split(v, "-");
                    int start = Integer.valueOf(startAndEnd[0]);
                    int end = Integer.valueOf(startAndEnd[1]);
                    for (int i = start; i <= end; i++) {
                        clusterNode.getServedSlotsSet().add(Integer.valueOf(i));
                    }
                }
            });

            clusterNodes.add(clusterNode);
        }

        return clusterNodes;
    }

    public String slotsDisplay(Collection<Integer> slots) {
        if (slots == null || slots.isEmpty()) {
            return "";
        }

        List<String> result = Lists.newArrayList();

        int i = 0;
        int first = 0;
        int last = 0;
        for (int current : Sets.newTreeSet(slots)) {
            // if first loop
            if (i == 0) {
                if (slots.size() == 1) {
                    result.add(String.valueOf(current));
                    break;
                }

                first = current;
                last = current;
                i++;
                continue;
            }

            if (current == last + 1) {
                // if last loop
                if (i == slots.size() - 1) {
                    result.add(new StringBuilder().append(first).append("-").append(current).toString());
                    break;
                }

                last = current;
                i++;
                continue;
            } else {
                // if last loop
                if (i == slots.size() - 1) {
                    if (first == last) {
                        result.add(String.valueOf(first));
                    } else {
                        result.add(new StringBuilder().append(first).append("-").append(last).toString());
                    }
                    result.add(String.valueOf(current));
                    break;
                }

                if (first == last) {
                    result.add(String.valueOf(first));
                } else {
                    result.add(new StringBuilder().append(first).append("-").append(last).toString());
                }
                first = current;
                last = current;
                i++;
                continue;
            }
        }

        return StringUtils.join(result, ",");
    }

    public Set<String> getHostAndPorts(List<String> hostAndPortRanges) {
        Set<String> hostAndPorts = Sets.newTreeSet();

        hostAndPortRanges.forEach(v -> {
            v = StringUtils.trim(v);
            ValidationUtils.hostAndPortRange(v);
            String[] hostAndPortRangeArray = StringUtils.split(v, ":");

            String[] portRangeArray = StringUtils.split(hostAndPortRangeArray[1], "-");
            int start = Integer.valueOf(portRangeArray[0]);
            int end;
            if (portRangeArray.length > 1) {
                end = Integer.valueOf(portRangeArray[1]);
            } else {
                end = start;
            }
            if (start > end) {
                throw new InvalidParameterException(String.format("%s is invalid. start port must be equal or less than end port.", v));
            }
            for (int i = start; i <= end; i++) {
                hostAndPorts.add(new StringBuilder().append(hostAndPortRangeArray[0]).append(":").append(i).toString());
            }
        });

        return hostAndPorts;
    }

    public Set<Integer> getSlots(List<String> slotsStr) {
        Set<Integer> slots = Sets.newTreeSet();

        slotsStr.forEach(v -> {
            v = StringUtils.trim(v);

            String[] slotArray = StringUtils.split(v, "-");
            ValidationUtils.numeric(slotArray[0], "Slot");
            int start = Integer.valueOf(slotArray[0]);
            int end;
            if (slotArray.length > 1) {
                ValidationUtils.numeric(slotArray[1], "Slot");
                end = Integer.valueOf(slotArray[1]);
            } else {
                end = start;
            }
            if (start > end) {
                throw new InvalidParameterException(String.format("%s is invalid. start slot must be equal or less than end slot.", v));
            }
            for (int i = start; i <= end; i++) {
                slots.add(i);
            }
        });

        return slots;
    }
}
