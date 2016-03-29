package com.behase.relumin.controller;

import com.behase.relumin.exception.InvalidParameterException;
import com.behase.relumin.model.Cluster;
import com.behase.relumin.model.ClusterNode;
import com.behase.relumin.model.param.CreateClusterParam;
import com.behase.relumin.service.ClusterService;
import com.behase.relumin.service.LoggingOperationService;
import com.behase.relumin.service.NodeService;
import com.behase.relumin.service.RedisTribService;
import com.behase.relumin.webconfig.WebConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.doReturn;

@Slf4j
public class RedisTribApiControllerTest {
    @InjectMocks
    @Spy
    private RedisTribApiController controller = new RedisTribApiController();

    @Mock
    private RedisTribService redisTibService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private NodeService nodeService;

    @Mock
    private LoggingOperationService loggingOperationService;

    @Spy
    private ObjectMapper mapper = WebConfig.MAPPER;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getCreateParameter_replicas_is_not_number_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("replicas must be number"));

        controller.getCreateParameter("hoge", "localhost:10000");
    }

    @Test
    public void getCreateParameter_hostAndPorts_is_blank_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("hostAndPorts must not be blank"));

        controller.getCreateParameter("1", "");
    }

    @Test
    public void getCreateParameter() throws Exception {
        doReturn(Lists.newArrayList()).when(redisTibService).getCreateClusterParams(anyInt(), any());

        List<CreateClusterParam> result = controller.getCreateParameter("1", "localhost:10000");
        log.info("result={}", result);
        assertThat(result, is(empty()));
    }

    @Test
    public void createCluster_with_clusterName_when_clusterName_already_exists_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("already exists"));

        doReturn(true).when(clusterService).existsClusterName(anyString());

        controller.createCluster(null, "test1", "");
    }

    @Test
    public void createCluster_with_clusterName_when_params_is_blank_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("params must not be blank"));

        doReturn(false).when(clusterService).existsClusterName(anyString());

        controller.createCluster(null, "test1", "");
    }

    @Test
    public void createCluster_with_clusterName_when_params_is_not_json_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("params is not JSON"));

        doReturn(false).when(clusterService).existsClusterName(anyString());

        controller.createCluster(null, "test1", "hoge");
    }

    @Test
    public void createCluster_with_clusterName() throws Exception {
        doReturn(false).when(clusterService).existsClusterName(anyString());
        doReturn(Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build())
                .when(clusterService)
                .getCluster("test1");

        Cluster result = controller.createCluster(null, "test1", "[{}]");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void createCluster_when_params_is_blank_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("params must not be blank"));

        doReturn(false).when(clusterService).existsClusterName(anyString());

        controller.createCluster(null, "");
    }

    @Test
    public void createCluster_when_params_is_not_json_then_throw_exception() throws Exception {
        expectedEx.expect(InvalidParameterException.class);
        expectedEx.expectMessage(containsString("params is not JSON"));

        doReturn(false).when(clusterService).existsClusterName(anyString());

        controller.createCluster(null, "hoge");
    }

    @Test
    public void createCluster() throws Exception {
        doReturn(Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build())
                .when(clusterService)
                .getClusterByHostAndPort(anyString());

        Cluster result = controller.createCluster(null, "[{}]");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void checkCluster() throws Exception {
        doReturn(new ClusterNode()).when(clusterService).getActiveClusterNode(anyString());
        doReturn(Lists.newArrayList("hoge")).when(redisTibService).checkCluster(anyString());

        Map<String, List<String>> result = controller.checkCluster("test1");
        log.info("result={}", result);
        assertThat(result.get("errors"), contains("hoge"));
    }

    @Test
    public void reshardCluster() throws Exception {
        Cluster cluster = Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build();

        doReturn(new ClusterNode()).when(clusterService).getActiveClusterNode(anyString());
        doReturn(cluster).when(clusterService).getClusterByHostAndPort(anyString());

        Cluster result = controller.reshardCluster(null, "", "1", "", "");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void reshardClusterBySlots() throws Exception {
        Cluster cluster = Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build();

        doReturn(new ClusterNode()).when(clusterService).getActiveClusterNode(anyString());
        doReturn(cluster).when(clusterService).getClusterByHostAndPort(anyString());

        Cluster result = controller.reshardClusterBySlots(null, "", "", "");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void addNode() throws Exception {
        Cluster cluster = Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build();

        doReturn(new ClusterNode()).when(clusterService).getActiveClusterNode(anyString());
        doReturn(cluster).when(clusterService).getClusterByHostAndPort(anyString());

        Cluster result = controller.addNode(null, "", "", "");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void deleteNode_isFail_true() throws Exception {
        Cluster cluster = Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build();

        doReturn(new ClusterNode()).when(clusterService).getActiveClusterNodeWithExcludeNodeId(anyString(), anyString());
        doReturn(cluster).when(clusterService).getClusterByHostAndPort(anyString());

        Cluster result = controller.deleteNode(null, "", "", "true", "", "");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void deleteNode_isFail_false_shutdown_is_true() throws Exception {
        Cluster cluster = Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build();

        doReturn(new ClusterNode()).when(clusterService).getActiveClusterNodeWithExcludeNodeId(anyString(), anyString());
        doReturn(cluster).when(clusterService).getClusterByHostAndPort(anyString());

        Cluster result = controller.deleteNode(null, "", "", "false", "", "true");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void replicateNode() throws Exception {
        Cluster cluster = Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build();

        doReturn(cluster).when(clusterService).getClusterByHostAndPort(anyString());

        Cluster result = controller.replicateNode(null, "", "");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void failoverNode() throws Exception {
        Cluster cluster = Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build();

        doReturn(cluster).when(clusterService).getClusterByHostAndPort(anyString());

        Cluster result = controller.failoverNode(null, "");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }

    @Test
    public void shutdown() throws Exception {
        Cluster cluster = Cluster.builder()
                .clusterName("test1")
                .info(Maps.newHashMap())
                .nodes(Lists.newArrayList())
                .build();

        doReturn(new ClusterNode()).when(clusterService).getActiveClusterNodeWithExcludeHostAndPort(anyString(), anyString());
        doReturn(cluster).when(clusterService).getClusterByHostAndPort(anyString());

        Cluster result = controller.shutdown(null, "", "");
        log.info("result={}", result);
        assertThat(result.getClusterName(), is("test1"));
    }
}
