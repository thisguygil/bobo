import os

import openai
from flask import Flask, request

boboAI = Flask(__name__)
openai.api_key = os.getenv("OPENAI_API_KEY")

messages = []


@boboAI.route("/generate", methods=["POST"])
def generate():
    prompt = request.form["prompt"]
    messages.append({"role": "user", "content": prompt})
    response = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        messages=messages,
        max_tokens=500,
    )
    output = response['choices'][0]['message']['content']
    messages.append({"role": "assistant", "content": output})
    return output


@boboAI.route("/reset", methods=["GET"])
def reset():
    messages.clear()
    return "Chat reset."
