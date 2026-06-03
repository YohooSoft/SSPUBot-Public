class NoElementFoundError(Exception):
    """Exception raised when no element is found in a data structure."""

    def __init__(self, message="No element found in the data structure."):
        self.message = message
        super().__init__(self.message)
