/*
   Copyright 2013 Ilya Lakhin (Илья Александрович Лахин)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package name.lakhin.eliah.projects
package papacarlo.syntax.rules

import name.lakhin.eliah.projects.papacarlo.syntax.{Session, Rule}
import name.lakhin.eliah.projects.papacarlo.syntax.Result._

final case class RepetitionRule(element: Rule,
                                separator: Option[Rule] = None,
                                min: Option[Int] = None,
                                max: Option[Int] = None) extends Rule {
  def apply(session: Session): Int = {
    session.syntax.onRuleEnter.trigger(this, session.state)

    val min = this.min.getOrElse(0)
    val max = this.max.getOrElse(Int.MaxValue)
    val initialState = session.state

    if (element(session) == Failed)
      if (min <= 0) {
        session.state = initialState
        session.syntax.onRuleLeave.trigger(this, session.state, Successful)
        return Successful
      } else {
        session.syntax.onRuleLeave.trigger(this, session.state, Failed)
        return Failed
      }

    var counter = 1
    var lastIssues = session.state.issues

    this.separator match {
      case Some(division: Rule) =>
        var finished = counter >= max

        while (!finished) {
          val lastState = session.state

          if (division(session) == Failed) {
            lastIssues = session.state.issues
            session.state = lastState
            finished = true
          }

          if (!finished && element(session) == Failed) {
            lastIssues = session.state.issues
            session.state = lastState
            finished = true
          }

          if (!finished) {
            counter += 1
            finished = counter >= max
          }
        }

      case _ =>
        var finished = counter >= max

        while (!finished) {
          val lastState = session.state

          if (element(session) == Failed) {
            lastIssues = session.state.issues
            session.state = lastState
            finished = true
          }

          if (!finished) {
            counter += 1
            finished = counter >= max
          }
        }
    }

    val result = if (counter >= min) Successful
    else {
      session.state = initialState.copy(issues = lastIssues)
      Failed
    }

    session.syntax.onRuleLeave.trigger(this, session.state, result)
    result
  }

  override val show =
    (((min, max) match {
      case (None, None) => element.showOperand(4) + "*"
      case (Some(0), None) => element.showOperand(4) + "*"
      case (Some(1), None) => element.showOperand(4) + "+"
      case (Some(min: Int), None) => element.showOperand(4) + " * " + min
      case (Some(min: Int), Some(max: Int)) => element.showOperand(4) + " * (" +
        min + ", " +  max + ")"
      case (None, Some(max: Int)) => element.showOperand(4) + " * (0, " +  max +
        ")"
    }) + separator.map(" / " + _.showOperand(4)).getOrElse("")) -> 4

  override val captures =
    element.captures ++ separator.map(_.captures).getOrElse(Set.empty)

  override val branches = element.branches
}
