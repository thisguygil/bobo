import os

import openai
from flask import Flask, request

boboAI = Flask(__name__)
openai.api_key = os.getenv("OPENAI_API_KEY")


@boboAI.route("/", methods=["POST"])
def generate():
    response = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "system", "content": "You are a helpful assistant."},
            {"role": "user", "content": request.form["prompt"]},
        ],
        max_tokens=300,
        temperature=0.6,
    )
    output = response['choices'][0]['message']['content']
    return output
