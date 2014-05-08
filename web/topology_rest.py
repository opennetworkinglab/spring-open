#! /usr/bin/env python
import os
import sys
import json
from urllib2 import Request, urlopen, URLError, HTTPError
from flask import Flask, json, Response, render_template, make_response, request

# The GUI can be accessed at <this_host>:9000/onos-topology.html

WEB_DIR = os.path.dirname(os.path.realpath(__file__))

LOCAL_CONFIG_FILE = os.path.join(WEB_DIR, "config.json")
DEFAULT_CONFIG_FILE = os.path.join(WEB_DIR, "config.json.default")

app = Flask(__name__)

def read_config():
  global guiIp, guiPort, onosIp, onosPort, controllers

  if (os.path.isfile(LOCAL_CONFIG_FILE)):
    confFile = open(LOCAL_CONFIG_FILE)
  else:
    print " * Local config file not found - loading default: %s" % DEFAULT_CONFIG_FILE
    print " * If you want to modify the config, copy %s to %s and make changes there" % (DEFAULT_CONFIG_FILE, LOCAL_CONFIG_FILE)
    print " "
    confFile = open(DEFAULT_CONFIG_FILE)
  
  conf = json.load(confFile)

  try:
    guiIp = conf['gui-ip']
    guiPort = conf['gui-port']
    onosIp = conf['onos-ip']
    onosPort = conf['onos-port']
    controllers = conf['controllers']
  except KeyError as e:
    print " Parameters were missing from the config file: %s" % e
    print " Your may be using an old version - please check your config matches the template in %s" % DEFAULT_CONFIG_FILE
    sys.exit(1)

  confFile.close()

## Worker Functions ##
def log_error(txt):
  print '%s' % (txt)

### File Fetch ###
@app.route('/ui/img/<filename>', methods=['GET'])
@app.route('/js/<filename>', methods=['GET'])
@app.route('/log/<filename>', methods=['GET'])
@app.route('/', methods=['GET'])
@app.route('/<filename>', methods=['GET'])
def return_file(filename="index.html"):
  if request.path == "/":
    fullpath = os.path.join(WEB_DIR, "onos-topology.html")
  else:
    fullpath = os.path.join(WEB_DIR, str(request.path)[1:])

  try:
    open(fullpath)
  except:
    response = make_response("Cannot find a file: %s" % (fullpath), 500)
    response.headers["Content-type"] = "text/html"
    return response

  response = make_response(open(fullpath).read())
  suffix = fullpath.split(".")[-1]

  if suffix == "html" or suffix == "htm":
    response.headers["Content-type"] = "text/html"
  elif suffix == "js":
    response.headers["Content-type"] = "application/javascript"
  elif suffix == "css":
    response.headers["Content-type"] = "text/css"
  elif suffix == "png":
    response.headers["Content-type"] = "image/png"
  elif suffix == "svg":
    response.headers["Content-type"] = "image/svg+xml"

  return response

###### ONOS REST API ##############################
## Worker Func ###
def get_json(url):
  code = 200;
  try:
    response = urlopen(url)
  except URLError, e:
    log_error("get_json: REST IF %s has issue. Reason: %s" % (url, e.reason))
    result = ""
    return (500, result)
  except HTTPError, e:
    log_error("get_json: REST IF %s has issue. Code %s" % (url, e.code))
    result = ""
    return (e.code, result)

  result = response.read()
  return (code, result)


def node_id(switch_array, dpid):
  id = -1
  for i, val in enumerate(switch_array):
    if val['name'] == dpid:
      id = i
      break

  return id

## API for ON.Lab local GUI ##
@app.route('/topology', methods=['GET'])
def topology_for_gui():
  try:
    url="http://%s:%s/wm/onos/topology/switches/json" % (onosIp, onosPort)
    (code, result) = get_json(url)
    parsedResult = json.loads(result)
  except:
    log_error("REST IF has issue: %s" % url)
    log_error("%s" % result)
    return

  topo = {}
  switches = []
  links = []
  devices = []

  for v in parsedResult:
    if v.has_key('dpid'):
#      if v.has_key('dpid') and str(v['state']) == "ACTIVE":#;if you want only ACTIVE nodes
      dpid = str(v['dpid'])
      state = str(v['state'])
      sw = {}
      sw['name']=dpid
      sw['group']= -1

      if state == "INACTIVE":
        sw['group']=0
      switches.append(sw)

  try:
    url="http://%s:%s/wm/onos/registry/switches/json" % (onosIp, onosPort)
    (code, result) = get_json(url)
    parsedResult = json.loads(result)
  except:
    log_error("REST IF has issue: %s" % url)
    log_error("%s" % result)

  for key in parsedResult:
    dpid = key
    ctrl = parsedResult[dpid][0]['controllerId']
    sw_id = node_id(switches, dpid)
    if sw_id != -1:
      if switches[sw_id]['group'] != 0:
        switches[sw_id]['group'] = controllers.index(ctrl) + 1

  try:
    url = "http://%s:%s/wm/onos/topology/links/json" % (onosIp, onosPort)
    (code, result) = get_json(url)
    parsedResult = json.loads(result)
  except:
    log_error("REST IF has issue: %s" % url)
    log_error("%s" % result)
    return

  for v in parsedResult:
    link = {}
    if v.has_key('dst-switch'):
      dst_dpid = str(v['dst-switch'])
      dst_id = node_id(switches, dst_dpid)
    if v.has_key('src-switch'):
      src_dpid = str(v['src-switch'])
      src_id = node_id(switches, src_dpid)
    link['source'] = src_id
    link['target'] = dst_id

    links.append(link)

  topo['nodes'] = switches
  topo['links'] = links

  js = json.dumps(topo)
  resp = Response(js, status=200, mimetype='application/json')
  return resp

@app.route("/controller_status")
def controller_status():
  url= "http://%s:%d/wm/onos/registry/controllers/json" % (onosIp, onosPort)
  (code, result) = get_json(url)
  parsedResult = json.loads(result)

  cont_status=[]
  for i in controllers:
    status={}
    if i in parsedResult:
      onos=1
    else:
      onos=0
    status["name"]=i
    status["onos"]=onos
    cont_status.append(status)

  js = json.dumps(cont_status)
  resp = Response(js, status=200, mimetype='application/json')
  return resp

if __name__ == "__main__":
  read_config()
  #app.debug = True
  app.run(threaded=True, host=guiIp, port=guiPort)
