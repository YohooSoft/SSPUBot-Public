import toml
import openai

from SSPUBot.Ai.AiCore import get_LNLPM_response

config = toml.load('./data/config_dont_upload.toml')


def check_content_worker(prompt, contentA, contentB):
    """
    :param prompt: 比较任务的提示信息
    :param contentA: A 内容
    :param contentB: B 内容
    :return: 比较结果，指出两者的差异和相似之处
    """
    print(f"Using model: qwen/qwen-2.5-vl-7b-instruct:free for content comparison.")
    try:
        html_content = get_LNLPM_response(
            prompt=f"""
            {prompt}
            内容A:
            {contentA}
            内容B:
            {contentB}
            """,
            systemContent=config["SSPUWebSiteUsesRequestsInfoSource"]["system_content"]["task3"],
            model="qwen/qwen-2.5-vl-7b-instruct:free"
        )
        return html_content
    except openai.RateLimitError:
        print(f"Rate limit exceeded for model: {i}. Trying next model if available.")
    return "Error: All models rate limited or failed."
