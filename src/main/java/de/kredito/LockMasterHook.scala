package de.kredito

import java.util

import com.atlassian.stash.commit.CommitService
import com.atlassian.stash.content.ChangesetsBetweenRequest
import com.atlassian.stash.hook.HookResponse
import com.atlassian.stash.hook.repository.{PreReceiveRepositoryHook, RepositoryHookContext}
import com.atlassian.stash.repository.{RefChange, Repository}
import com.atlassian.stash.server.ApplicationPropertiesService
import com.atlassian.stash.util.{PageRequest, PageRequestImpl}

import scala.collection.JavaConversions._

class LockMasterHook(commitService: CommitService, applicationPropertiesService: ApplicationPropertiesService)
  extends PreReceiveRepositoryHook {

  override def onReceive(p1: RepositoryHookContext, p2: util.Collection[RefChange], p3: HookResponse): Boolean = {
    val res = p2.flatMap(getMessages(p1.getRepository)).forall(_.startsWith(ALLOWED_PREFIX))
    p3.out().printf(PATTERN, if (res) OK else ERROR)
    res
  }

  def getMessages(repository: Repository)(refChange: RefChange) = {
    refChange.getRefId match {
      case "refs/heads/master" =>
        val request = new ChangesetsBetweenRequest.Builder(repository)
          .exclude(refChange.getFromHash)
          .include(refChange.getToHash)
          .build()

        val page = commitService.getChangesetsBetween(request, new PageRequestImpl(0, PageRequest.MAX_PAGE_LIMIT))
        val msgs = page.getValues.map(_.getMessage)
        msgs
      case _ => Seq()
    }


  }

  private val ALLOWED_PREFIX = "[hotfix]"
  private val OK = "OK to push refs"
  private val ERROR = "Master branch is locked. Only hotfixes are allowed"
  private val PATTERN =
    """~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
      |~                                                      ~
      |~  %-51s ~
      |~                                                      ~
      |~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    """.stripMargin
}