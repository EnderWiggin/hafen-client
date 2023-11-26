from __future__ import annotations
from typing import Optional

from .PBotCharacterAPI import PBotCharacterAPI
from .PBotGobAPI import PBotGobAPI
from .PBotUtils import PBotUtils
from .PBotWindowAPI import PBotWindowAPI


class PBotSession(object):
    def __init__(self, session):
        self.session = session
        self.PBotUtils: PBotUtils = PBotUtils(session)
        self.PBotGobAPI: PBotGobAPI = PBotGobAPI(session)
        self.PBotWindowAPI: PBotWindowAPI = PBotWindowAPI(session)
        self.PBotCharacterAPI: PBotCharacterAPI = PBotCharacterAPI(session)

    ## Close the session, create new session with new_session after this
    def close_session(self):
        self.session.closeSession()

    ## Create new session return None if unsuccessful
    # @param username username to log in with
    # @param password password to log in with
    # @param charname charname to log in with
    # @return PBot session created or None if unsuccessful
    def new_session(self, username: str, password: str, charname: str) -> Optional[PBotSession]:
        sess = self.session.newSession(username, password, charname)
        if sess is None:
            return None
        else:
            return PBotSession(sess)
