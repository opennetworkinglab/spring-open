package net.onrc.onos.apps.segmentrouting.web;


public class SegmentRouterPolicyRESTParams {
    private String policy_id;
    private String policy_type;
    private int priority;
    private String tunnel_id;
    private String proto_type;
    private String src_ip;
    private String src_tp_port_op;
    private short src_tp_port;
    private String dst_ip;
    private String dst_tp_port_op;
    private short dst_tp_port;

    public SegmentRouterPolicyRESTParams() {
        this.policy_id = null;
        this.policy_type = null;
        this.priority = 0;
        this.tunnel_id = null;
        this.proto_type = null;
        this.src_ip = null;
        this.src_tp_port_op = null;
        this.src_tp_port = 0;
        this.dst_ip = null;
        this.dst_tp_port_op = null;
        this.dst_tp_port = 0;
    }

    public void setPolicy_id(String policy_id) {
        this.policy_id = policy_id;
    }

    public String getPolicy_id() {
        return this.policy_id;
    }

    public void setPolicy_type(String policy_type) {
        this.policy_type = policy_type;
    }

    public String getPolicy_type() {
        return this.policy_type;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return this.priority;
    }

    public void setTunnel_id(String tunnel_id) {
        this.tunnel_id = tunnel_id;
    }

    public String getTunnel_id() {
        return this.tunnel_id;
    }

    public void setProto_type(String proto_type) {
        this.proto_type = proto_type;
    }

    public String getProto_type() {
        return this.proto_type;
    }

    public void setSrc_ip(String src_ip) {
        this.src_ip = src_ip;
    }

    public String getSrc_ip() {
        return this.src_ip;
    }

    public void setSrc_tp_port_op(String src_tp_port_op) {
        this.src_tp_port_op = src_tp_port_op;
    }

    public String getSrc_tp_port_op() {
        return this.src_tp_port_op;
    }

    public void setSrc_tp_port(short src_tp_port) {
        this.src_tp_port = src_tp_port;
    }

    public short getSrc_tp_port() {
        return this.src_tp_port;
    }

    public void setDst_ip(String dst_ip) {
        this.dst_ip = dst_ip;
    }

    public String getDst_ip() {
        return this.dst_ip;
    }

    public void setDst_tp_port_op(String dst_tp_port_op) {
        this.dst_tp_port_op = dst_tp_port_op;
    }

    public String getDst_tp_port_op() {
        return this.dst_tp_port_op;
    }

    public void setDst_tp_port(short dst_tp_port) {
        this.dst_tp_port = dst_tp_port;
    }

    public short getDst_tp_port() {
        return this.dst_tp_port;
    }
}
