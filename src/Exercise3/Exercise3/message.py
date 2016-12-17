class Message(object):
    """Describes a single message"""

    sender = 0
    level = 0
    id = 0

    def __init__(self, sender, level, id):
        self.sender = sender
        self.level = level
        self.id = id

