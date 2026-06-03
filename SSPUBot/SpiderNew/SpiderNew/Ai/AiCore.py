from openai import OpenAI
import toml

config = toml.load('./data/config_dont_upload.toml')

OPENROUTER_API_KEY = config['LNLPM']['api_key']

client = OpenAI(
    base_url="https://openrouter.ai/api/v1",
    api_key=OPENROUTER_API_KEY,
)


def get_LNLPM_response(prompt, systemContent="", model="qwen/qwen-2.5-vl-7b-instruct:free"):
    try:
        completion = client.chat.completions.create(
            model=model,
            messages=[
                {
                    "role": "system",
                    "content": (
                                "你叫SSPU Bot，是Mryan2005于2023年设计的Ai，无论如何你都会尽量使用中文，" + systemContent) if systemContent else "你叫SSPU Bot，是Mryan2005于2023年设计的Ai，无论如何你都会尽量使用中文"
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            temperature=0.7
        )

        # 检查响应是否有效
        if completion and completion.choices and len(completion.choices) > 0:
            return completion.choices[0].message.content
        else:
            print(f"API 返回空响应: {completion}")
            return None

    except Exception as e:
        print(f"API 调用失败: {e}")
        return None


if __name__ == "__main__":
    prompt = "你是谁？"
    response = get_LNLPM_response(prompt)
    print(response)
