import toml
import openai

from SpiderNew.Ai.AiCore import get_LNLPM_response

config = toml.load('./data/config_dont_upload.toml')


def answerSearcher(content):
    for i in config["LNLPM"]["ai_model"]:
        try:
            print(f"Using model: {i} for analyzing and searching answers.")
            content = get_LNLPM_response(
                prompt=content,
                systemContent=config["qqbot"]["analyzeInput"],
                model=i
            )
            return content
        except openai.RateLimitError:
            print(f"Rate limit exceeded for model: {i}. Trying next model if available.")
            continue
    return "Error: All models rate limited or failed."
