class Congress:
    @staticmethod
    def check_summarized_content(prompt, contentA, contentB):
        return check_content_worker(prompt, contentA, contentB)

    @staticmethod
    def check_tags_generation(tags):
        return check_tags_generate()

    @staticmethod
    def check_simplified_content(prompt, contentA, contentB):
        return check_content_worker(prompt, contentA, contentB)
