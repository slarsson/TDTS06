import urllib.parse

TARGETTYPE = ['text/plain', 'text/html']

def parse(data):
    text = data.decode('latin1')
    headers_raw, body = text.split('\r\n\r\n')
    headers_raw = headers_raw.split('\r\n')

    headers = {}
    for row in headers_raw[1:]:
        pair  = row.split(':', 1)       
        headers[pair[0].lower()] = pair[1].strip()

    # responses such as 304 does not have a body    
    if 'content-length' not in headers:
        return data
    
    # check if all data has been recived
    if int(headers['content-length']) != len(body):
        return False
    
    # split content-type to handle cases with text/plain; utf-8
    contenttype = headers['content-type'].split(';')[0]
    if contenttype in TARGETTYPE:
        text = text.replace('Stockholm', 'Linkoping')
        text = text.replace('Smiley', 'Trolly')
    return text.encode('latin1')

def check_input(first_row):
    if first_row[0] != 'GET':
        return False
    if first_row[2] != 'HTTP/1.1' and first_row != 'HTTP/1.0':
        return False
    try:
        host = urllib.parse.urlparse(first_row[1])
        return (host.hostname, host.port)
    except Exception as e:
        print(e, host, first_row)
    return False

def get_first_row(data):
    text = data.decode('latin1')
    first_row = text.split('\n')[0]
    items = first_row.split(sep=None)
    if len(items) != 3:
        return False
    return items

def replace_url(url, data):
    if url == 'http://zebroid.ida.liu.se/fakenews/smiley.jpg':
        text = data.decode('latin1')
        text = text.replace('http://zebroid.ida.liu.se/fakenews/smiley.jpg', 'http://zebroid.ida.liu.se/fakenews/trolly.jpg')
        return text.encode('latin1')
    return data