from SSPUBot.dataStructureSTL.Exceptions import *


class Queue:
    def __init__(self):
        self.items = []

    def is_empty(self):
        return len(self.items) == 0

    def push(self, item):
        self.items.append(item)

    def pop(self):
        if not self.is_empty():
            return self.items.pop(0)
        raise NoElementFoundError("Dequeue from empty queue")

    def size(self):
        return len(self.items)

    def peek(self):
        if not self.is_empty():
            return self.items[0]
        raise IndexError("Peek from empty queue")
