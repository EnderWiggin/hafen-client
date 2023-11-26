import sys
import os
sys.path.insert(0, os.path.dirname(os.path.realpath(__file__)))
import importlib
import threading
from py4j.java_gateway import JavaGateway, CallbackServerParameters, GatewayParameters
from  __pbot.PBotSession import PBotSession

class PBotRunner(object):

    def start(scriptName, pBotSession):
        importlib.invalidate_caches()
        script = importlib.import_module(scriptName)
        importlib.reload(script)
        threading.Thread(target=script.Script().run, args=[PBotSession(pBotSession)]).start()
    class Java:
        implements = ["haven.purus.pbot.Py4j.PBotScriptLoader"]
gateway = JavaGateway(callback_server_parameters=CallbackServerParameters(),
                      python_server_entry_point=PBotRunner,
                      gateway_parameters=GatewayParameters(auto_field=True, auto_convert=True))
print("0")
