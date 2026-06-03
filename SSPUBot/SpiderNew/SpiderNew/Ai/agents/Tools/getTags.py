import toml
import openai

from SpiderNew.Ai.AiCore import get_LNLPM_response

config = toml.load('./data/config_dont_upload.toml')


def generate_tags(html_content):
    for i in config["LNLPM"]["ai_model"]:
        try:
            print(f"Using model: {i} for tag generation.")
            html_content = get_LNLPM_response(
                prompt=html_content,
                systemContent=config["SSPUWebSiteUsesRequestsInfoSource"]["system_content"]["task1"],
                model=i
            )
            return html_content
        except openai.RateLimitError:
            print(f"Rate limit exceeded for model: {i}. Trying next model if available.")
            continue
    return "Error: All models rate limited or failed."
