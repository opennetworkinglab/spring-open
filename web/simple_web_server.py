#! /usr/bin/env python
import json
import argparse
import time
import re
from urllib2 import Request, urlopen, URLError, HTTPError
from flask import Flask, make_response, request

## Global Var for ON.Lab local REST ##
RestIP="localhost"
RestPort=8080
DEBUG=1

app = Flask(__name__)

### Serving Static Files ###
@app.route('/', methods=['GET'])
@app.route('/<filename>', methods=['GET'])
@app.route('/js/<filename>', methods=['GET'])
def return_file(filename="/"):
  if request.path == "/":
    fullpath = "./simple-topo.html"
  else:
    fullpath = str(request.path)[1:]

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

## Proxying REST calls ###
@app.route('/wm/', defaults={'path':''})
@app.route('/wm/<path:path>')
def rest(path):
  url="http://%s:%s/wm/%s" % (RestIP, RestPort, path)
  try:
    response = urlopen(url)
  except URLError, e:
    errorString = "Could not access ONOS resource %s: %s" % (url, e.reason)
    print errorString
    return errorString, 500
  except HTTPError, e:
    errorString = "Error during ONOS resource request %s: %s" % (url, e.code)
    print errorString
    return errorString, e.code

  result = response.read()
  return result

if __name__ == "__main__":
  app.debug = True
  app.run(threaded=True, host="0.0.0.0", port=9000)
