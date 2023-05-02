import os

import openai
from flask import Flask, request

boboAI = Flask(__name__)
openai.api_key = os.getenv("OPENAI_API_KEY")


def initialize_messages():
    return [{"role": "system",
             "content": """You are Bobo, a fun and helpful Discord bot created by Gil Anavi. You use slash commands 
             and provide music, chat, and other features. Don't refer to yourself as an AI language model. When users 
             call you with the 'chat' command, engage with them. For help, direct users to the 'help' command."""}]


messages = initialize_messages()


@boboAI.route("/generate", methods=["POST"])
def generate():
    global messages
    prompt = request.form["prompt"]
    messages.append({"role": "user", "content": prompt})
    response = openai.ChatCompletion.create(
        model="gpt-3.5-turbo",
        messages=messages,
        max_tokens=350,
    )
    output = response['choices'][0]['message']['content']
    messages.append({"role": "assistant", "content": output})
    return output


@boboAI.route("/reset", methods=["GET"])
def reset():
    global messages
    messages = initialize_messages()
    return "Chat reset."
