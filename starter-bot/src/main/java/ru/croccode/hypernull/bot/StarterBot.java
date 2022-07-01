package ru.croccode.hypernull.bot;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

import ru.croccode.hypernull.domain.MatchMode;
import ru.croccode.hypernull.geometry.Offset;
import ru.croccode.hypernull.geometry.Point;
import ru.croccode.hypernull.io.SocketSession;
import ru.croccode.hypernull.message.Hello;
import ru.croccode.hypernull.message.MatchOver;
import ru.croccode.hypernull.message.MatchStarted;
import ru.croccode.hypernull.message.Move;
import ru.croccode.hypernull.message.Register;
import ru.croccode.hypernull.message.Update;
import ru.croccode.hypernull.reader.PropertiesReader;

public class StarterBot implements Bot {

	private static final Random rnd = new Random(System.currentTimeMillis());

	private Offset moveOffset;

	private int moveCounter = 0;

	private final Map<String, String> properties;

	private int numRounds, mapWidth, mapHeight, id, viewRadius, miningRadius, attackRadius;

	public StarterBot(Map<String, String> properties) {
		this.properties = properties;
	}

	private MatchMode getMode() {
		return Objects.equals(properties.get("mode"), "FRIENDLY") ?
				MatchMode.FRIENDLY : MatchMode.DEATHMATCH;
	}

	private int getDirection(int a, int aTo) {
		return -Integer.signum(a - aTo);
	}

	private Offset nearestCoinOffset(int myX, int myY, int toX, int toY) {
		return new Offset(
				getDirection(myX, toX),
				getDirection(myY, toY)
		);
	}

	@Override
	public Register onHello(Hello hello) {
		Register register = new Register();
		register.setMode(getMode());
		register.setBotName(properties.get("bot.name"));
		register.setBotSecret(properties.get("bot.secret"));
		return register;
	}

	@Override
	public void onMatchStarted(MatchStarted matchStarted) {
		// TODO:
		// ничего не понятно
		numRounds = matchStarted.getNumRounds();
		mapWidth = matchStarted.getMapSize().width();
		mapHeight = matchStarted.getMapSize().height();
		id = matchStarted.getYourId();
		viewRadius = matchStarted.getViewRadius();
		miningRadius = matchStarted.getMiningRadius();
		attackRadius = matchStarted.getAttackRadius();
	}

	private boolean iAmStuck = false;

	@Override
	public Move onUpdate(Update update) {
//		if (moveOffset == null || moveCounter > 5 + rnd.nextInt(5)) {
//			moveOffset = new Offset(
//					rnd.nextInt(3) - 1,
//					rnd.nextInt(3) - 1
//			);
//			moveCounter = 0;
//		}

		// TODO:
		// умные вещи
		Set<Point> blocks = update.getBlocks();
		Map<Integer, Point> bots = update.getBots();
		Set<Point> coins = update.getCoins();

		int myPointX = bots.get(id).x(), myPointY = bots.get(id).y();

		if (iAmStuck) {
			// moveOffset = stuckOffset();
		}
		else {
			if (!Objects.equals(coins, null) && !coins.isEmpty()) {
				Optional<Point> nearestCoin = coins.stream().min(Comparator.comparingInt(
						p -> (
								(p.x() - myPointX) * (p.x() - myPointX) +
										(p.y() - myPointY) * (p.y() - myPointY)
						)
				));

				Point trueNearestCoin = nearestCoin.get();
				moveOffset = nearestCoinOffset(myPointX, myPointY, trueNearestCoin.x(), trueNearestCoin.y());
			}
			else {
				moveOffset = new Offset(
					rnd.nextInt(3) - 1,
					rnd.nextInt(3) - 1
				);
			}
		}

		moveCounter++;
		Move move = new Move();
		move.setOffset(moveOffset);
		return move;
	}

	@Override
	public void onMatchOver(MatchOver matchOver) {
	}

	public static void main(String[] args) throws IOException {
		Socket socket = new Socket();
		socket.setTcpNoDelay(true);
		socket.setSoTimeout(300_000);

		Map<String, String> propertiesTmp = PropertiesReader.readProperties(args[0]);

		socket.connect(
				new InetSocketAddress(
						propertiesTmp.get("server.host"),
						Integer.parseInt(propertiesTmp.get("server.port"))
				)
		);

		SocketSession session = new SocketSession(socket);

		StarterBot bot = new StarterBot(
				propertiesTmp
		);

		new BotMatchRunner(bot, session).run();
	}
}
