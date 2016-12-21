from ex3 import Transport

class NodeInfo(object):
    """description of class"""

    id = None
    connect_uri = None
    bind_uri = None
    local = False

    def __init__(self, id: int, transport: Transport, host: str, port: int, local: bool = False):
        self.id = id
        self.connect_uri = "{0}://{1}:{2}".format(transport,host,port)
        self.bind_uri = "{0}://*:{1}".format(transport,port)
        self.local = local
