from ex3 import NodeInfo
class Message(object):
    """Describes a single message"""

    src = None
    dst = None
    level = None
    id = None

    def __init__(self, src: NodeInfo, dst: NodeInfo, level: int, id: int):
        self.src = src
        self.dst = dst
        self.level = level
        self.id = id
