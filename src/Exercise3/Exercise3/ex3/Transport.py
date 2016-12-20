from enum import Enum
class Transport(Enum):
    def __str__(self):
        return str(self.value)

    TCP = 'tcp'
    InProc = 'inproc'
    UDP = 'udp'
