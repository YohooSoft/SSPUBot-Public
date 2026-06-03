import toml
import openai

from SpiderNew.Ai.AiCore import get_LNLPM_response

config = toml.load('./data/config_dont_upload.toml')


def transform_html_to_markdown(html_content):
    for i in config["LNLPM"]["ai_model"]:
        try:
            print(f"Using model: {i} for HTML to Markdown transformation.")
            result = get_LNLPM_response(
                prompt=html_content,
                systemContent=config["SSPUWebSiteUsesRequestsInfoSource"]["system_content"]["task0_transformer"],
                model=i
            )
            if result is not None:
                return result
            else:
                print(f"Model {i} returned None. Trying next model if available.")
                continue
        except Exception as e:
            print(f"Error with model {i}: {e}. Trying next model if available.")
            continue
    return "Error: All models rate limited or failed."
