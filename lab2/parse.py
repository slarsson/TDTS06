import urllib.parse
import re
from bs4 import BeautifulSoup, NavigableString

TEXT_ENCODING = 'latin1'

def parse(data):
    text = data.decode(TEXT_ENCODING)
    headers_raw, body = text.split('\r\n\r\n')
    
    headers = {}
    for row in headers_raw.split('\r\n')[1:]:
        pair  = row.split(':', 1)
        headers[pair[0].lower()  ] = pair[1].strip()

    # responses such as 304 does not have a body    
    if 'content-length' not in headers:
        return data
    
    # check if all data has been recived
    if int(headers['content-length']) != len(body):
        return False
    
    contenttype = headers['content-type'].split(';')[0]
    if contenttype == 'text/html':
        soup = BeautifulSoup(body, features='lxml')
        for tag in soup.find_all():
            for item in tag.contents:
                if isinstance(item, NavigableString):
                    txt = item.string.replace('Stockholm', 'LinkaN')
                    txt = txt.replace('Smiley', 'Trolly')
                    item.string.replace_with(txt)
        new_body = str(soup)
        new_headers = re.sub(r'(?i)(?<=content-length:)(\s+\d+)', ' '+str(len(new_body)), headers_raw) 
        text = new_headers + '\r\n\r\n' + new_body     
    elif 'text/plain':
        body = body.replace('Stockholm', 'Linkoping')
        body = body.replace('Smiley', 'Trolly')
        text = headers_raw + '\r\n\r\n' + body
    
    return text.encode(TEXT_ENCODING)

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
    text = data.decode(TEXT_ENCODING)
    first_row = text.split('\n')[0]
    items = first_row.split(sep=None)
    if len(items) != 3:
        return False
    return items

def replace_url(url, data):
    if url == 'http://zebroid.ida.liu.se/fakenews/smiley.jpg':
        text = data.decode(TEXT_ENCODING)
        text = text.replace('http://zebroid.ida.liu.se/fakenews/smiley.jpg', 'http://zebroid.ida.liu.se/fakenews/trolly.jpg')
        return text.encode(TEXT_ENCODING)
    return data