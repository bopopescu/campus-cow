'''
Simple Flask application to test deployment to Amazon Web Services
Uses Elastic Beanstalk and RDS

Author: Scott Rodkey - rodkeyscott@gmail.com

Step-by-step tutorial: https://medium.com/@rodkey/deploying-a-flask-application-on-aws-a72daba6bb80
'''

from flask import Flask, jsonify, json, render_template, request, Response
from application import db
from application.models import Data
from application.forms import EnterDBInfo, RetrieveDBInfo

# Elastic Beanstalk initalization
application = Flask(__name__)
application.debug=True

############################
# Personal Data
mDict = {}
iPlayers = 0
isCow = 0

# change this to your own value
application.secret_key = 'cC1YCIWOj9GgWspgNEo2'   

@application.route('/', methods=['GET', 'POST'])
@application.route('/index', methods=['GET', 'POST'])
def index():
    form1 = EnterDBInfo(request.form) 
    form2 = RetrieveDBInfo(request.form) 
    
    if request.method == 'POST' and form1.validate():
        data_entered = Data(notes=form1.dbNotes.data)
        try:     
            db.session.add(data_entered)
            db.session.commit()        
            db.session.close()
        except:
            db.session.rollback()
        return render_template('thanks.html', notes=form1.dbNotes.data)
        
    if request.method == 'POST' and form2.validate():
        try:   
            num_return = int(form2.numRetrieve.data)
            query_db = Data.query.order_by(Data.id.desc()).limit(num_return)
            for q in query_db:
                print(q.notes)
            db.session.close()
        except:
            db.session.rollback()
        return render_template('results.html', results=query_db, num_return=num_return)                
    
    return render_template('index.html', form1=form1, form2=form2)


# ############################
# # Initialize
@application.route("/getid", methods=["GET", "POST"])
def getid():
    global iPlayers
    iPlayers = iPlayers + 1
    return jsonify(
        id=iPlayers
    )

@application.route("/totalids", methods=["GET", "POST"])
def totalids():
    global iPlayers
    global isCow
    return jsonify(
        num=iPlayers,
        cow=isCow
    )

# ############################
# # Cow
@application.route("/createcow", methods=["GET", "POST"])
def createcow():
    global isCow
    if isCow == 0:
        isCow = 1
        return jsonify (
            cow=0
        )
    else:
        return jsonify (
            cow=1
        )

# ############################
# # GPS
@application.route("/api/<loc>", methods=["GET", "POST"])
def location(loc):
    temp = tuple(loc.split(","))
    mDict[int(temp[0])] = (float(temp[1]), float(temp[2]))
    mList = []
    for key in mDict:
        mDictTemp = {}
        mDictTemp["id"] = key
        mDictTemp["lat"] = (mDict[key][0])
        mDictTemp["lon"] = (mDict[key][1])
        mList.append(mDictTemp)

    return json.dumps(mList) #jsonify(mList)   

@application.route("/empty", methods=["GET", "POST"])
def empty():
    global iPlayers
    global isCow
    mDict.clear()
    iPlayers=0
    isCow=0
    return jsonify(
        dummy=0
    )

if __name__ == '__main__':
    application.run(host='0.0.0.0')
