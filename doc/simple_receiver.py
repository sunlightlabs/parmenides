from flask import Flask, jsonify
app = Flask(__name__)

@app.route("/status")
def status():
    return jsonify(status="listening")

@app.route("/resolve",methods=['POST'])
def resolve():
   jsondata = request.form("jsondata")
   print(jsondata)
   return jsonify(resolved=true)

if __name__ == "__main__":
    app.run(host='0.0.0.0',debug=True)
