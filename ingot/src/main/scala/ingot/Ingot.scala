package ingot

import cats.data.{ EitherT, StateT }
import cats.Applicative
import cats.syntax.either._

object Ingot {
  outer =>

  final class RightTPartiallyApplied[F[_], S, L] {
    def apply[R](x: R)(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](st => A.pure((st, Either.right[L, R](x))))
  }

  def pure[F[_], S, L] = new RightTPartiallyApplied[F, S, L]

  def apply[F[_], S, L, R](
    x: StateWithLogs[S] => F[(StateWithLogs[S], Either[L, R])])(
    implicit
    A: Applicative[F]): Ingot[F, S, L, R] =
    EitherT[StateT[F, StateWithLogs[S], ?], L, R](StateT(x))

  final class RightPartiallyApplied[S, L] {
    def apply[F[_], R](x: F[R])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](s => A.map(x)(x => (s, Either.right[L, R](x))))
  }

  def liftF[S, L] = new RightPartiallyApplied[S, L]

  def rightT[F[_], S, L] = new RightTPartiallyApplied[F, S, L]

  final class LeftTPartiallyApplied[F[_], S, R] {
    def apply[L](x: L)(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](s => A.pure((s, Either.left[L, R](x))))
  }

  def leftT[F[_], S, R] = new LeftTPartiallyApplied[F, S, R]

  def right[S, L] = new RightPartiallyApplied[S, L]

  final class LeftPartiallyApplied[S, R] {
    def apply[F[_], L](x: F[L])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](s => A.map(x)(x => (s, Either.left[L, R](x))))
  }

  def left[S, R] = new LeftPartiallyApplied[S, R]

  final class LiftPartiallyApplied[S] {
    def apply[F[_], L, R](x: F[Either[L, R]])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](s => A.map(x)(x => (s, x)))
  }

  def lift[S] = new LiftPartiallyApplied[S]

  final class GuardPartiallyApplied[G[_], S] {
    def apply[F[_], R](x: F[R])(implicit A: Applicative[G], G: Guard[F, G]): Ingot[G, S, Throwable, R] =
      outer.apply[G, S, Throwable, R](s => A.map(G(x))(x => (s, x)))
  }

  def guard[G[_], S] = new GuardPartiallyApplied[G, S]

  def log[F[_], S, L](x: LogMessage)(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
    apply[F, S, L, Unit](s => A.pure((s.combine(x), Either.right[L, Unit](()))))

  def log[F[_], S, L](x: Logs)(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
    apply[F, S, L, Unit](s => A.pure(((s.combine(x), Either.right[L, Unit](())))))

  def get[F[_], S, L](implicit A: Applicative[F]): Ingot[F, S, L, S] =
    apply[F, S, L, S](s => A.pure((s, Either.right[L, S](s.state))))

  def getL[F[_], S, L](implicit A: Applicative[F]): Ingot[F, S, L, Logs] =
    apply[F, S, L, Logs](s => A.pure((s, Either.right[L, Logs](s.logs))))

  final class SetPartiallyApplied[F[_], L] {
    def apply[S](x: S)(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
      outer.apply[F, S, L, Unit](s => A.pure(((s.copy(state = x), Either.right[L, Unit](())))))
  }
  def set[F[_], L] = new SetPartiallyApplied[F, L]

  final class SetFPartiallyApplied[L] {
    def apply[F[_], S](x: F[S])(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
      outer.apply[F, S, L, Unit](s => A.map(x)(x => (s.copy(state = x), Either.right[L, Unit](()))))
  }

  def setF[L] = new SetFPartiallyApplied[L]

  final class ModifyPartiallyApplied[F[_], L] {
    def apply[S](f: S => S)(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
      outer.apply[F, S, L, Unit](s => A.pure(((s.copy(state = f(s.state)), Either.right[L, Unit](())))))
  }
  def modify[F[_], L] = new ModifyPartiallyApplied[F, L]

  final class ModifyFPartiallyApplied[L] {
    def apply[F[_], S](f: S => F[S])(implicit A: Applicative[F]): Ingot[F, S, L, Unit] =
      outer.apply[F, S, L, Unit](s => A.map(f(s.state))(x => (s.copy(state = x), Either.right[L, Unit](()))))
  }

  def modifyF[L] = new ModifyFPartiallyApplied[L]

  final class InspectPartiallyApplied[F[_]] {
    def apply[S, L, R](f: S => Either[L, R])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](s => A.pure((s, f(s.state))))
  }

  def inspect[F[_]] = new InspectPartiallyApplied[F]

  final class InspectEPartiallyApplied[F[_], L] {
    def apply[S, R](f: S => R)(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](s => A.pure((s, Either.right[L, R](f(s.state)))))
  }

  def inspectE[F[_], L] = new InspectEPartiallyApplied[F, L]

  def inspectF[F[_], S, L, R](f: S => F[Either[L, R]])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.map(f(s.state))(r => (s, r)))

  final class InspectGPartiallyApplied[G[_]] {
    def apply[F[_], S, R](
      f: S => F[R])(
      implicit
      A: Applicative[G],
      G: Guard[F, G]): Ingot[G, S, Throwable, R] =
      outer.apply[G, S, Throwable, R](s => A.map(G(f(s.state)))(r => (s, r)))
  }

  def inspectG[G[_]] = new InspectGPartiallyApplied[G]

  final class InspectLPartiallyApplied[F[_]] {
    def apply[S, L, R](f: S => (Logs, Either[L, R]))(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](s => A.pure {
        val (logs, r) = f(s.state)
        (s.combine(logs), r)
      })
  }

  def inspectL[F[_]] = new InspectLPartiallyApplied[F]

  final class InspectELPartiallyApplied[F[_], L] {
    def apply[S, R](f: S => (Logs, R))(implicit A: Applicative[F]): Ingot[F, S, L, R] =
      outer.apply[F, S, L, R](s => A.pure {
        val (logs, r) = f(s.state)
        (s.combine(logs), Either.right[L, R](r))
      })
  }

  def inspectEL[F[_], L] = new InspectELPartiallyApplied[F, L]

  def inspectFL[F[_], S, L, R](f: S => F[(Logs, Either[L, R])])(implicit A: Applicative[F]): Ingot[F, S, L, R] =
    apply[F, S, L, R](s => A.map(f(s.state)) { case (logs, r) => (s.combine(logs), r) })

  def flushLogs[F[_], S, L](
    logger: Logger[F])(
    implicit
    A: Applicative[F]): Ingot[F, S, L, Unit] = Ingot[F, S, L, Unit] { swl =>
    A.map(logger.log(swl.logs))(_ => (StateWithLogs.init(swl.state), Either.right[L, Unit](())))
  }

}
