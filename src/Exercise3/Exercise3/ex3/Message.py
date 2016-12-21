from ex3 import NodeInfo
import random
class Message(object):
    """Describes a single message"""

    src = None
    dst = None
    level = None
    id = None
    rand = None

    def __init__(self, src: NodeInfo, dst: NodeInfo, level: int, id: int):
        self.src = src
        self.dst = dst
        self.level = level
        self.id = id
        self.rand = random.randint(0,100000)
