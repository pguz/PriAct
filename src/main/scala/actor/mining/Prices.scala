package actor.mining

/**
 * Created by klis on 15.05.15.
 */
class Prices(val prices: List[Double]) {
  def average = prices.sum / prices.length
}
