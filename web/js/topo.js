function convert_to_topodata(switches, links, registry){
    var controllers={};
    var nr_controllers=0;
    var sws=[];
    var ls=[];
    var topo = new Array();
    switches.forEach(function(item) {
	var sw={}
	sw.name=item.dpid;
	sw.group=-1;
	sws.push(sw);
    });
    for (var r in registry){
	if ( ! (registry[r][0]['controllerId'] in controllers) ){
	    controllers[registry[r][0]['controllerId']] = ++nr_controllers;
	}
    }
    for (var i = 0; i < sws.length; i++){
	if (sws[i].name in registry){
	    sws[i].group=controllers[registry[sws[i].name][0]['controllerId']];
	    sws[i].controller=registry[sws[i].name][0]['controllerId'];
	}
    }
    links.forEach(function(item) {
	var link={};
	for (var i = 0; i < sws.length; i++){
	    if(sws[i].name == item['src']['dpid'])
		break;
	}
	link.source=i;
	for (var i = 0; i < sws.length; i++){
	    if(sws[i].name == item['dst']['dpid'])
		break;
	}
	link.target=i;
	ls.push(link);
    });
    topo['nodes']=sws;
    topo['links']=ls;
    return topo;
}

var width = 1280;
var height = 1280;
var radius = 8;
function gui(switch_url, link_url, registry_url){
    var svg = d3.select("#topology")
	.append("svg:svg")
	.attr("width", width)
	.attr("height", height);

    var status_header = svg.append("svg:text")
        .attr("id", "status_header")
        .attr("x", 50)
	.attr("y", 20);

    var force = d3.layout.force()
	.charge(-500)
	.linkDistance(100)
	.size([width, height]);

    var node_drag = d3.behavior.drag()
        .on("dragstart", dragstart)
        .on("drag", dragmove)
        .on("dragend", dragend);

    var color = d3.scale.category20();
    var topodata;
    var nodes = force.nodes();
    var links = force.links();

    d3.json(switch_url, function(error, rest_switches) {
	d3.json(link_url, function(error, rest_links) { 
	    d3.json(registry_url, function(error, rest_registry) { 
		topodata = convert_to_topodata(rest_switches, rest_links, rest_registry);
		init(topodata, nodes, links);
		path = svg.append("svg:g").selectAll("path").data(links);
		circle = svg.append("svg:g").selectAll("circle").data(nodes);
		text = svg.append("svg:g").selectAll("text.node-label").data(nodes);
		draw();
	    });
	}); 
    }); 

    setInterval(function(){ 
	d3.json(switch_url, function(error, rest_switches) {
	    d3.json(link_url, function(error, rest_links) { 
		d3.json(registry_url, function(error, rest_registry) { 
		    topodata = convert_to_topodata(rest_switches, rest_links, rest_registry);
		    var changed = update(topodata, nodes, links);
		    path = svg.selectAll("path").data(links)
		    circle = svg.selectAll("circle").data(nodes);
		    text = svg.selectAll("text.node-label").data(nodes);
		    if ( changed ){
			draw();
		    }
		});
	    }); 
	}); 
    }, 3000); 

    function draw(){
	force.stop();
        svg.select("#status_header")
            .text(function(){return "Switch: " + force.nodes().length + " (Active: " + nr_active_sw()  + ")/ Link: " + force.links().length});

	path.enter().append("svg:path")
	    .attr("class", function(d) { return "link"; })
	    .attr("marker-end", function(d) {
		if(d.type == 1){
		    return "url(#TriangleRed)";
		} else {
		    return "url(#Triangle)";
		}
	    });
	
	circle.enter().append("svg:circle")
	    .attr("r", function(d) { 
		if (d.group == 1000){
		    return radius;
		}else{
		    return radius;
		}
	    })
	    .call(node_drag);
    
	text.enter().append("svg:text")
            .classed("node-label", true)
	    .attr("x", radius)
	    .attr("y", ".31em")
	    .text(function(d) { 
		l=d.name.split(":").length
		return d.name.split(":")[l-2] + ":" + d.name.split(":")[l-1]
	    });
    
	circle.append("title")
	    .text(function(d) { return d.name; });
    
	circle.attr("fill", function(d) {
            if (d.group == 1){
		return "red"
            }else if (d.group == 2){
		return "blue"
            }else if (d.group == 3){
		return "green"
            }else if (d.group == 4){
		return "orange"
            }else if (d.group == 5){
		return "cyan"
            }else if (d.group == 6){
		return "magenta"
            }else if (d.group == 7){
		return "yellow"
            }else if (d.group == 8){
		return "purple"
            }else{
		return "gray"
            }
	});

	path.attr("stroke", function(d) {
	    if(d.type == 1){
		return "red"
	    } else {
		return "black"
	    }
	}).attr("stroke-width", function(d) {
	    if(d.type == 1){
		return "2px";
	    } else {
		return "1.5px";
	    }
	}).attr("marker-end", function(d) {
	    if(d.type == 1){
		return "url(#TriangleRed)";
	    } else {
		return "url(#Triangle)";
	    }
	});
	path.exit().remove();
	circle.exit().remove();
	text.exit().remove();
	force.on("tick", tick);
	force.start();
    }
    function nr_active_sw(){
        var n=0; 
        var nodes = force.nodes();
        for(var i=0;i<nodes.length;i++){
          if(nodes[i].group!=0)
            n++;
        }; 
        return n;
    }
    function dragstart(d, i) {
        force.stop() // stops the force auto positioning before you start dragging
    }
    function dragmove(d, i) {
        d.px += d3.event.dx;
        d.py += d3.event.dy;
        d.x += d3.event.dx;
        d.y += d3.event.dy; 
        tick(); // this is the key to make it work together with updating both px,py,x,y on d !
    }

    function dragend(d, i) {
        d.fixed = true; // of course set the node to fixed so the force doesn't include the node in its auto positioning stuff
        tick();
        force.resume();
    }
    function tick() {
	path.attr("d", function(d) {
	    var dx = d.target.x - d.source.x,
	    dy = d.target.y - d.source.y,
	    dr = 1/d.linknum;  //linknum is defined above
	    dr = 0;  // 0 for direct line
	    return "M" + d.source.x + "," + d.source.y + "A" + dr + "," + dr + " 0 0,1 " + d.target.x + "," + d.target.y;
	});
	path.attr("stroke", function(d) {
	    if(d.type == 1){
		return "red"
	    } else {
		return "black"
	    }
	}).attr("stroke-width", function(d) {
	    if(d.type == 1){
		return "3px";
	    } else {
		return "1.5px";
	    }
	}).attr("marker-end", function(d) {
	    if(d.type == 1){
		return "url(#TriangleRed)";
	    } else {
		return "url(#Triangle)";
	    }
	});
//	circle.attr("cx", function(d) { return d.x; }).attr("cy", function(d) { return d.y; });
	circle.attr("transform", function(d) {
	    x = Math.max(radius, Math.min(width - radius, d.x));
	    y = Math.max(radius, Math.min(height - radius, d.y)); 
//	    return "translate(" + d.x + "," + d.y + ")";
	    return "translate(" + x + "," + y + ")";
	})
	circle.attr("fill", function(d) {
                if (d.group == 1){
                    return "red"
                }else if (d.group == 2){
                    return "blue"
                }else if (d.group == 3){
                    return "green"
                }else if (d.group == 4){
                    return "orange"
                }else if (d.group == 5){
                    return "cyan"
                }else if (d.group == 6){
                    return "magenta"
                }else if (d.group == 7){
                    return "yellow"
                }else if (d.group == 8){
                    return "purple"
                }else{
                    return "gray"
                }
	});
//	text.attr("x", function(d) { return d.x; }).attr("y", function(d) { return d.y; });
	text.attr("transform", function(d) {
	    return "translate(" + d.x + "," + d.y + ")";
	});
    }

}


function init(topodata, nodes, links){
    topodata.nodes.forEach(function(item) {
        nodes.push(item);
    });
    topodata.links.forEach(function(item) {
        links.push(item);
    });
    links.sort(compare_link);
    // distinguish links that have the same src & dst node by 'linknum'
    for (var i=1; i < links.length; i++) {
        if (links[i].source == links[i-1].source &&
            links[i].target == links[i-1].target) {
            links[i].linknum = links[i-1].linknum + 1;
        } else {
	    links[i].linknum = 1;
	};
    };
}

function compare_link (a, b){
    if (a.source > b.source)
	return 1;
    else if (a.source < b.source)
	return -1;
    else {
        if (a.target > b.target)
	    return 1;
        else if (a.target < b.target)
	    return -1;
        else
	    return 0;
    }
}

/* Return nodes that is not in the current list of nodes */
Array.prototype.node_diff = function(arr) {
    return this.filter(function(i) {
	for (var j = 0; j < arr.length ; j++) {
	    if (arr[j].name === i.name)
		return false;
	}
	return true;
    });
};

/* Return removed links */
function gone_links (topo_json, links){
    gone = []
    for (var i = 0; i < links.length ; i ++){
	var found = 0;
	for (var j = 0; j < topo_json.links.length ; j ++){
	    if (links[i].source.name == topo_json.nodes[topo_json.links[j].source].name && 
		links[i].target.name == topo_json.nodes[topo_json.links[j].target].name ){
		found = 1;
		break;
	    }
	}
	if ( found == 0 ){
	    gone.push(links[i]);
	}
    }
    return gone;
}

/* Return added links */
function added_links (topo_json, links) {
    added = [];
    for (var j = 0; j < topo_json.links.length ; j ++){
	var found = 0;
	for (var i = 0; i < links.length ; i ++){
	    if (links[i].source.name == topo_json.nodes[topo_json.links[j].source].name && 
		links[i].target.name == topo_json.nodes[topo_json.links[j].target].name ){
		found = 1;
		break;
	    }
	}
	if ( found == 0 ){
	    added.push(topo_json.links[j]);
	}
    }
    return added;
}

/* check if toplogy has changed and update node[] and link[] accordingly */
function update(json, nodes, links){
    var changed = false;
    var n_adds = json.nodes.node_diff(nodes);
    var n_rems = nodes.node_diff(json.nodes);
    for (var i = 0; i < n_adds.length; i++) {
	nodes.push(n_adds[i]);
	changed = true;
    }
    for (var i = 0; i < n_rems.length; i++) {
	for (var j = 0; j < nodes.length; j++) {
	    if ( nodes[j].name == n_rems[i].name ){
		nodes.splice(j,1);
		changed = true;
		break;
	    }
	}
    }
    var l_adds = added_links(json, links);
    var l_rems = gone_links(json, links);
    for (var i = 0; i < l_rems.length ; i++) {
	for (var j = 0; j < links.length; j++) {
            if (links[j].source.name == l_rems[i].source.name &&
		links[j].target.name == l_rems[i].target.name) {
		links.splice(j,1);
		changed = true;
		break;
            }
	}
    }
    // Sorce/target of an element of l_adds[] are corresponding to the index of json.node[]
    // which is different from the index of node[] (new nodes are always added to the last)
    // So update soure/target node indexes of l_add[] need to be fixed to point to the proper
    // node in node[];
    for (var i = 0; i < l_adds.length; i++) {
	for (var j = 0; j < nodes.length; j++) {
	    if ( json.nodes[l_adds[i].source].name == nodes[j].name ){
		l_adds[i].source = j; 
		break;
	    }
	}
	for (var j = 0; j < nodes.length; j++) {
	    if ( json.nodes[l_adds[i].target].name == nodes[j].name ){
		l_adds[i].target = j;
		break;
	    }
	}
	links.push(l_adds[i]);
	changed = true;
    }

    // Update "group" attribute of nodes
    for (var i = 0; i < nodes.length; i++) {
        for (var j = 0; j < json.nodes.length; j++) {
	    if ( nodes[i].name == json.nodes[j].name ){
		if (nodes[i].group != json.nodes[j].group){
		    nodes[i].group = json.nodes[j].group;
		    changed = true;
		}
	    }
	}
    }
    for (var i = 0; i < links.length; i++) {
        for (var j = 0; j < json.links.length; j++) {
	    if (links[i].target.name == json.nodes[json.links[j].target].name && 
		links[i].source.name == json.nodes[json.links[j].source].name ){
		if (links[i].type != json.links[j].type){
		    links[i].type = json.links[j].type;
		    changed = true;
		}
	    }
	}
    }
    return changed
}

