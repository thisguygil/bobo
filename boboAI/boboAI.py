import os

import openai
from flask import Flask, request

boboAI = Flask(__name__)
openai.api_key = os.getenv("OPENAI_API_KEY")


@boboAI.route("/", methods=["POST"])
def generate():
    response = openai.Completion.create(
        model="text-davinci-003",
        prompt=request.form["prompt"],
        max_tokens=300,
        temperature=0.6
    )
    return response.choices[0].text
