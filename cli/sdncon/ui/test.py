from showswitch import show_switch_data
import json, urllib2

def main():
    request = None
    print show_switch_data(None)

    #url = 'http://localhost:8000/rest/v1/model/switch-alias/'
    #print json.load(urllib2.urlopen(url))

if __name__ == "__main__":
    main()